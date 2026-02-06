package io.github.fpt.ktorfit.ksp

import io.github.fpt.ktorfit.annotations.Body
import io.github.fpt.ktorfit.annotations.DELETE
import io.github.fpt.ktorfit.annotations.Field
import io.github.fpt.ktorfit.annotations.FieldMap
import io.github.fpt.ktorfit.annotations.FormUrlEncoded
import io.github.fpt.ktorfit.annotations.GET
import io.github.fpt.ktorfit.annotations.HEAD
import io.github.fpt.ktorfit.annotations.HTTP
import io.github.fpt.ktorfit.annotations.Header
import io.github.fpt.ktorfit.annotations.Headers
import io.github.fpt.ktorfit.annotations.Multipart
import io.github.fpt.ktorfit.annotations.OPTIONS
import io.github.fpt.ktorfit.annotations.PATCH
import io.github.fpt.ktorfit.annotations.POST
import io.github.fpt.ktorfit.annotations.PUT
import io.github.fpt.ktorfit.annotations.Part
import io.github.fpt.ktorfit.annotations.PartMap
import io.github.fpt.ktorfit.annotations.Path
import io.github.fpt.ktorfit.annotations.Query
import io.github.fpt.ktorfit.annotations.QueryMap
import io.github.fpt.ktorfit.annotations.Streaming
import io.github.fpt.ktorfit.annotations.Url
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class KtorfitProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val methodSymbols = listOf(
            GET::class,
            POST::class,
            PUT::class,
            DELETE::class,
            PATCH::class,
            HEAD::class,
            OPTIONS::class,
            HTTP::class
        ).flatMap { resolver.getSymbolsWithAnnotation(it.qualifiedName!!) }

        val functions = methodSymbols
            .filterIsInstance<KSFunctionDeclaration>()
            .toList()
        if (functions.isEmpty()) return emptyList()

        val byParent = functions.groupBy { it.parentDeclaration as? KSClassDeclaration }
        byParent.forEach { (parent, methods) ->
            if (parent == null) return@forEach
            if (!parent.validate()) return@forEach
            generateImplForInterface(parent, methods)
        }

        return emptyList()
    }

    private fun generateImplForInterface(
        interfaceDecl: KSClassDeclaration,
        methods: List<KSFunctionDeclaration>,
    ) {
        if (interfaceDecl.classKind != ClassKind.INTERFACE) {
            logger.warn("@HTTP method annotations should be used in an interface", interfaceDecl)
            return
        }

        val packageName = interfaceDecl.packageName.asString()
        val interfaceName = interfaceDecl.simpleName.asString()
        val implName = "${interfaceName}Impl"

        val ktorfitClass = ClassName(
            "io.github.fpt.ktorfit.runtime",
            "Ktorfit"
        )

        val implType = TypeSpec.classBuilder(implName)
            .addSuperinterface(interfaceDecl.toClassName())
            .addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ClassName("kotlin", "ExperimentalStdlibApi"))
                    .build()
            )
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("ktorfit", ktorfitClass)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("ktorfit", ktorfitClass, KModifier.PRIVATE)
                    .initializer("ktorfit")
                    .build()
            )

        val registryClass = ClassName("io.github.fpt.ktorfit.runtime", "KtorfitRegistry")
        val implClass = ClassName(packageName, implName)
        val autoRegisterProperty = PropertySpec.builder(
            "_ktorfitAutoRegister$implName",
            UNIT,
            KModifier.PRIVATE
        ).addAnnotation(
            AnnotationSpec.builder(
                ClassName("io.github.fpt.ktorfit.annotations", "KtorfitEagerInit")
            ).build()
        ).addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                .addMember("%T::class", ClassName("kotlin", "ExperimentalStdlibApi"))
                .build()
        ).addAnnotation(
            AnnotationSpec.builder(Suppress::class)
                .addMember("%S", "UNUSED_VARIABLE")
                .build()
        ).initializer(
            CodeBlock.of(
                "%T.register(%T::class) { %T(it) }",
                registryClass,
                interfaceDecl.toClassName(),
                implClass
            )
        ).build()

        val funSpecs = methods.map { method ->
            val httpInfo = resolveHttpInfo(method)
            val returnType = method.returnType?.toTypeName()
            if (returnType == null) {
                logger.error("Method return type missing", method)
            }

            val isSuspend = method.modifiers.contains(Modifier.SUSPEND)
            if (!isSuspend) {
                logger.error("@GET/@POST method must be suspend", method)
            }

            val params = method.parameters
            val hasField = params.any { it.hasAnnotation(Field::class.java) }
            val hasFieldMap = params.any { it.hasAnnotation(FieldMap::class.java) }
            if ((hasField || hasFieldMap) && !httpInfo.formEncoded) {
                logger.warn("@Field/@FieldMap should be used with @FormUrlEncoded", method)
            }
            val hasPart = params.any { it.hasAnnotation(Part::class.java) }
            val hasPartMap = params.any { it.hasAnnotation(PartMap::class.java) }
            if ((hasPart || hasPartMap) && !httpInfo.multipart) {
                logger.warn("@Part/@PartMap should be used with @Multipart", method)
            }
            val bodyParam = params.firstOrNull { it.hasAnnotation(Body::class.java) }
            if ((httpInfo.multipart || httpInfo.formEncoded) && bodyParam != null) {
                logger.warn("@Body should not be used with @Multipart/@FormUrlEncoded", method)
            }
            if (bodyParam != null && !httpInfo.hasBody) {
                logger.warn("@Body is not supported by this HTTP method", method)
            }

        val responseType = method.returnType!!.toTypeName()

            FunSpec.builder(method.simpleName.asString())
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .addParameters(params.map { it.toParameterSpec() })
                .returns(returnType!!)
                .addCode(
                    buildCallBlock(
                        httpInfo = httpInfo,
                        params = params,
                        bodyParam = bodyParam,
                        methodHeaders = resolveHeaders(method),
                        responseType = responseType
                    )
                )
                .build()
        }

        val fileSpec = FileSpec.builder(packageName, implName)
            .addImport("kotlin.reflect", "typeOf")
            .addType(implType.addFunctions(funSpecs).build())
            .addProperty(autoRegisterProperty)
            .build()

        fileSpec.writeTo(
            codeGenerator,
            Dependencies(aggregating = false, interfaceDecl.containingFile!!)
        )
    }

    private fun resolveHttpInfo(method: KSFunctionDeclaration): HttpInfo {
        val httpAnno = method.annotations.firstOrNull { it.shortName.asString() == "HTTP" }
        val getAnno = method.annotations.firstOrNull { it.shortName.asString() == "GET" }
        val postAnno = method.annotations.firstOrNull { it.shortName.asString() == "POST" }
        val putAnno = method.annotations.firstOrNull { it.shortName.asString() == "PUT" }
        val deleteAnno = method.annotations.firstOrNull { it.shortName.asString() == "DELETE" }
        val patchAnno = method.annotations.firstOrNull { it.shortName.asString() == "PATCH" }
        val headAnno = method.annotations.firstOrNull { it.shortName.asString() == "HEAD" }
        val optionsAnno = method.annotations.firstOrNull { it.shortName.asString() == "OPTIONS" }

        val methodType = when {
            httpAnno != null -> httpAnno.arguments.firstOrNull()?.value as? String ?: "GET"
            getAnno != null -> "GET"
            postAnno != null -> "POST"
            putAnno != null -> "PUT"
            deleteAnno != null -> "DELETE"
            patchAnno != null -> "PATCH"
            headAnno != null -> "HEAD"
            optionsAnno != null -> "OPTIONS"
            else -> "GET"
        }

        val path = when {
            httpAnno != null -> httpAnno.arguments.getOrNull(1)?.value as? String ?: ""
            getAnno != null -> getAnno.arguments.firstOrNull()?.value as? String ?: ""
            postAnno != null -> postAnno.arguments.firstOrNull()?.value as? String ?: ""
            putAnno != null -> putAnno.arguments.firstOrNull()?.value as? String ?: ""
            deleteAnno != null -> deleteAnno.arguments.firstOrNull()?.value as? String ?: ""
            patchAnno != null -> patchAnno.arguments.firstOrNull()?.value as? String ?: ""
            headAnno != null -> headAnno.arguments.firstOrNull()?.value as? String ?: ""
            optionsAnno != null -> optionsAnno.arguments.firstOrNull()?.value as? String ?: ""
            else -> ""
        }

        val hasBody = if (httpAnno != null) {
            httpAnno.arguments.getOrNull(2)?.value as? Boolean ?: false
        } else {
            methodType == "POST" || methodType == "PUT" || methodType == "PATCH" || methodType == "DELETE"
        }

        val formEncoded = method.annotations.any {
            val name = it.shortName.asString()
            name == "FormUrlEncoded" || name == "FromUrlEncoded" || name == "FromUrlCoded"
        }
        val multipart = method.annotations.any { it.shortName.asString() == "Multipart" }
        val streaming = method.annotations.any { it.shortName.asString() == "Streaming" }
        return HttpInfo(methodType, path, formEncoded, multipart, hasBody, streaming)
    }

    private fun resolveHeaders(method: KSFunctionDeclaration): List<Pair<String, String>> {
        val headers = mutableListOf<Pair<String, String>>()
        method.annotations
            .filter { it.shortName.asString() == "Headers" }
            .forEach { annotation ->
                val values = annotation.arguments.firstOrNull()?.value as? List<*>
                values?.forEach { rawValue ->
                    val raw = rawValue as? String ?: return@forEach
                    val idx = raw.indexOf(':')
                    if (idx <= 0 || idx == raw.lastIndex) {
                        logger.warn("@Headers value should be in 'Key: Value' format", method)
                        return@forEach
                    }
                    val key = raw.take(idx).trim()
                    val value = raw.substring(idx + 1).trim()
                    if (key.isBlank()) {
                        logger.warn("@Headers key is blank", method)
                        return@forEach
                    }
                    headers.add(key to value)
                }
            }
        return headers
    }

    private fun buildCallBlock(
        httpInfo: HttpInfo,
        params: List<KSValueParameter>,
        bodyParam: KSValueParameter?,
        methodHeaders: List<Pair<String, String>>,
        responseType: com.squareup.kotlinpoet.TypeName,
    ): CodeBlock {
        val headers = CodeBlock.builder()
            .addStatement("val headers = mutableListOf<Pair<String, String>>()")
            .build()
        val query = CodeBlock.builder()
            .addStatement("val query = linkedMapOf<String, String>()")
            .build()
        val fields = CodeBlock.builder()
            .addStatement("val fields = linkedMapOf<String, String>()")
            .build()
        val parts = CodeBlock.builder()
            .addStatement("val parts = linkedMapOf<String, Any?>()")
            .build()

        val block = CodeBlock.builder()
        block.addStatement("var path = %S", httpInfo.path)
        block.add(headers)
        block.add(query)
        block.add(fields)
        block.add(parts)

        methodHeaders.forEach { (key, value) ->
            block.addStatement("headers.add(%S to %S)", key, value)
        }

        params.forEach { param ->
            val name = param.name!!.asString()
            val queryAnno = param.findAnnotation(Query::class.java)
            val queryMapAnno = param.findAnnotation(QueryMap::class.java)
            val fieldAnno = param.findAnnotation(Field::class.java)
            val fieldMapAnno = param.findAnnotation(FieldMap::class.java)
            val pathAnno = param.findAnnotation(Path::class.java)
            val partAnno = param.findAnnotation(Part::class.java)
            val partMapAnno = param.findAnnotation(PartMap::class.java)
            val headerAnno = param.findAnnotation(Header::class.java)
            val urlAnno = param.findAnnotation(Url::class.java)

            if (urlAnno != null) {
                block.addStatement("if (%L != null) path = %L.toString()", name, name)
            }

            if (queryAnno != null) {
                val key = queryAnno.arguments.firstOrNull()?.value as? String
                val queryName = if (key.isNullOrBlank()) name else key
                block.addStatement(
                    "if (%L != null) query[%S] = %L.toString()",
                    name,
                    queryName,
                    name
                )
            }
            if (queryMapAnno != null) {
                block.beginControlFlow("if (%L != null)", name)
                block.addStatement(
                    "%L.forEach { (k, v) -> if (v != null) query[k] = v.toString() }",
                    name
                )
                block.endControlFlow()
            }
            if (fieldAnno != null) {
                val key = fieldAnno.arguments.firstOrNull()?.value as? String
                val fieldName = if (key.isNullOrBlank()) name else key
                if (httpInfo.formEncoded) {
                    block.addStatement(
                        "if (%L != null) fields[%S] = %L.toString()",
                        name,
                        fieldName,
                        name
                    )
                }
            }
            if (fieldMapAnno != null) {
                block.beginControlFlow("if (%L != null)", name)
                if (httpInfo.formEncoded) {
                    block.addStatement(
                        "%L.forEach { (k, v) -> if (v != null) fields[k] = v.toString() }",
                        name
                    )
                }
                block.endControlFlow()
            }
            if (pathAnno != null) {
                val key = pathAnno.arguments.firstOrNull()?.value as? String
                val pathName = if (key.isNullOrBlank()) name else key
                block.addStatement("path = path.replace(%S, %L.toString())", "{$pathName}", name)
            }
            if (partAnno != null) {
                val key = partAnno.arguments.firstOrNull()?.value as? String
                val partName = if (key.isNullOrBlank()) name else key
                block.addStatement("parts[%S] = %L", partName, name)
            }
            if (partMapAnno != null) {
                block.beginControlFlow("if (%L != null)", name)
                block.addStatement("%L.forEach { (k, v) -> parts[k] = v }", name)
                block.endControlFlow()
            }
            if (headerAnno != null) {
                val key = headerAnno.arguments.firstOrNull()?.value as? String
                val headerName = if (key.isNullOrBlank()) name else key
                block.beginControlFlow("if (%L != null)", name)
                block.addStatement("headers.removeAll { it.first == %S }", headerName)
                block.addStatement("headers.add(%S to %L.toString())", headerName, name)
                block.endControlFlow()
            }
        }

        val bodyArg = bodyParam?.name?.asString()
        val canUseBodyParam = httpInfo.hasBody &&
                bodyArg != null &&
                !httpInfo.formEncoded &&
                !httpInfo.multipart

        val bodyExpr = if (canUseBodyParam) bodyArg else "null"
        val formFieldsExpr =
            if (httpInfo.formEncoded) "if (fields.isEmpty()) null else fields" else "null"
        val multipartExpr =
            if (httpInfo.multipart) "if (parts.isEmpty()) null else parts" else "null"

        val requestCall = CodeBlock.of(
            "ktorfit.httpClient.request(%S, ktorfit.resolveUrl(path), headers, query, %L, $formFieldsExpr, $multipartExpr, %L, typeOf<%T>())",
            httpInfo.method,
            bodyExpr,
            httpInfo.streaming,
            responseType
        )
        block.addStatement("return %L as %T", requestCall, responseType)

        return block.build()
    }

    private fun KSValueParameter.toParameterSpec(): ParameterSpec {
        return ParameterSpec.builder(
            name!!.asString(),
            type.toTypeName()
        ).build()
    }

    private fun KSValueParameter.hasAnnotation(annotation: Class<*>): Boolean {
        val targetName = annotation.canonicalName
        return annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == targetName }
    }

    private fun KSValueParameter.findAnnotation(annotation: Class<*>): com.google.devtools.ksp.symbol.KSAnnotation? {
        val targetName = annotation.canonicalName
        return annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == targetName
        }
    }

    private data class HttpInfo(
        val method: String,
        val path: String,
        val formEncoded: Boolean,
        val multipart: Boolean,
        val hasBody: Boolean,
        val streaming: Boolean,
    )
}
