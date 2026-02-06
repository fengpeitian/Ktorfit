package io.github.fpt.ktorfit.sample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform