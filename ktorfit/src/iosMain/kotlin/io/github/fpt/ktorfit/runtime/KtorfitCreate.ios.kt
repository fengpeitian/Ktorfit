package io.github.fpt.ktorfit.runtime

import kotlin.reflect.KClass

actual fun <T : Any> Ktorfit.create(clazz: KClass<T>): T {
    return KtorfitRegistry.create(clazz, this)
        ?: throw UnsupportedOperationException(
            "Ktorfit.create failed on iOS: no registry entry for ${clazz.qualifiedName}."
        )
}
