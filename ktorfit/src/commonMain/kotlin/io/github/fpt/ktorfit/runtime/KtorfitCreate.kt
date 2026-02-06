package io.github.fpt.ktorfit.runtime

import kotlin.reflect.KClass

expect fun <T : Any> Ktorfit.create(clazz: KClass<T>): T
