package io.github.fpt.ktorfit.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.fpt.ktorfit.runtime.create
import io.github.fpt.ktorfit.sample.network.ToolsApi
import io.github.fpt.ktorfit.sample.network.createDefaultKtorfit
import io.github.fpt.ktorfit.sample.network.data.Article
import org.jetbrains.compose.resources.painterResource

import ktorfit.ktorfit_sample.generated.resources.Res
import ktorfit.ktorfit_sample.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val ktorfit = createDefaultKtorfit()
            val api = ktorfit.create(ToolsApi::class)
            val sentence = api.getSentence().data
            println("FPT-test: ${sentence}")

            val article = Article.article
            val resp = api.publishArticle(article)
            println("FPT-test: ${resp.code}, ${resp.message}")
            println("FPT-test: ${resp.data}")
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }

            AnimatedVisibility(showContent) {
                val greeting = remember { getPlatform() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}