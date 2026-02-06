package io.github.fpt.ktorfit.sample

class DesktopPlatform : Platform {
    override val name: String = "Desktop"
}

actual fun getPlatform(): Platform = DesktopPlatform()