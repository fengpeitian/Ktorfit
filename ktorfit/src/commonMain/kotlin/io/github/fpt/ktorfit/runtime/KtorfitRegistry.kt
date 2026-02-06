package io.github.fpt.ktorfit.runtime

import kotlin.reflect.KClass

object KtorfitRegistry {
    private val creators = mutableMapOf<KClass<*>, (Ktorfit) -> Any>()

    fun <T : Any> register(clazz: KClass<T>, creator: (Ktorfit) -> T) {
        creators[clazz] = creator
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> create(clazz: KClass<T>, ktorfit: Ktorfit): T? {
        return creators[clazz]?.invoke(ktorfit) as? T
    }
}
