package io.github.fpt.ktorfit.runtime

import kotlin.reflect.KClass

actual fun <T : Any> Ktorfit.create(clazz: KClass<T>): T {
    KtorfitRegistry.create(clazz, this)?.let { return it }
    val implName = clazz.qualifiedName + "Impl"
    val implClass = Class.forName(implName).asSubclass(clazz.java)
    val ctor = implClass.getConstructor(Ktorfit::class.java)
    return ctor.newInstance(this)
}
