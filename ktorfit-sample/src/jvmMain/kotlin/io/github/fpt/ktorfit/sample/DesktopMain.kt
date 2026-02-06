package io.github.fpt.ktorfit.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Ktorfit Sample"
    ) {
        App()
    }
}
