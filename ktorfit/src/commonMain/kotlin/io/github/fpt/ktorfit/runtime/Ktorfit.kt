package io.github.fpt.ktorfit.runtime

class Ktorfit(
    val baseUrl: String,
    val httpClient: KtorfitHttpClient
) {
    fun withBaseUrl(newBaseUrl: String): Ktorfit {
        return Ktorfit(newBaseUrl, httpClient)
    }

    fun resolveUrl(path: String): String {
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            baseUrl.trimEnd('/') + path
        }
    }
}
