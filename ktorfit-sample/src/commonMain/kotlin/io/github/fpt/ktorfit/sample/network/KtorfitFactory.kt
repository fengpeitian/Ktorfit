package io.github.fpt.ktorfit.sample.network

import io.github.fpt.ktorfit.runtime.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.collections.contains

private const val BASE_URL = "https://api.apiopen.top"

fun createDefaultKtorfit(
    baseUrl: String = BASE_URL,
    httpClient: HttpClient = createDefaultKtorHttpClient()
): Ktorfit {
    return Ktorfit(
        baseUrl = baseUrl,
        httpClient = KtorfitKtorClient(httpClient)
    )
}

fun createDefaultKtorHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                encodeDefaults = true
                isLenient = true
                coerceInputValues = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 30_000L
            requestTimeoutMillis = 30_000L
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }

            level = LogLevel.BODY

            // 白名单：只记录这些类型的log
            val allowedTypes = setOf(
                ContentType.Application.Json,
                ContentType.Application.FormUrlEncoded,
                ContentType.Text.Any,
                ContentType.Application.Xml,
                ContentType.Application.Xml_Dtd
            )

            filter { requestBuilder ->
                requestBuilder.contentType() in allowedTypes
            }
        }

        defaultRequest {
//            自己电脑的ip，给模拟器用的
//            host = "192.168.27.160"
//            port = 8080
            url(BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }
}
