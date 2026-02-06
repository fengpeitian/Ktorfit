package io.github.fpt.ktorfit.sample.network

import io.github.fpt.ktorfit.runtime.KtorfitHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KType

class KtorfitKtorClient(
    private val client: HttpClient,
    private val json: Json = defaultJson()
) : KtorfitHttpClient {

    override suspend fun request(
        method: String,
        url: String,
        headers: List<Pair<String, String>>,
        query: Map<String, String>,
        body: Any?,
        formFields: Map<String, String>?,
        multipartParts: Map<String, Any?>?,
        streaming: Boolean,
        responseType: KType
    ): Any {
        val response = client.request(url) {
            this.method = HttpMethod.parse(method)
            url {
                query.forEach { (k, v) -> parameters.append(k, v) }
            }
            headers.forEach { (k, v) -> header(k, v) }

            if (multipartParts != null) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            multipartParts.forEach { (k, v) ->
                                when (v) {
                                    null -> {}
                                    is ByteArray -> append(k, v, Headers.Empty)
                                    else -> append(k, v.toString())
                                }
                            }
                        }
                    )
                )
            } else if (formFields != null) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(parameters {
                    formFields.forEach { (k, v) -> append(k, v) }
                }))
            } else if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }

        if (streaming) {
            return response.body<ByteArray>() as Any
        }

        val text = response.bodyAsText()
        return json.decodeFromString(json.serializersModule.serializer(responseType), text) as Any
    }
}

fun defaultJson(): Json {
    return Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}
