package io.github.fpt.ktorfit.runtime

import kotlin.reflect.KType

interface KtorfitHttpClient {
    suspend fun request(
        method: String,
        url: String,
        headers: List<Pair<String, String>>,
        query: Map<String, String>,
        body: Any?,
        formFields: Map<String, String>?,
        multipartParts: Map<String, Any?>?,
        streaming: Boolean,
        responseType: KType
    ): Any
}
