package io.github.fpt.ktorfit.sample.network.data

import kotlinx.serialization.Serializable

/**
 * @author : fengpeitian
 * e-mail  : fengpeitian@xunlei.com
 * time    : 2026/1/30 16:12
 * desc    :
 */
@Serializable
data class Article(
    val category: String?,
    val content: String?,
    val cover_image: String?,
    val is_pinned: Boolean,
    val summary: String?,
    val tags: String?,
    val title: String?,
) {
    companion object {

        val article = Article(
            "技术",
            "Go 是一门由 Google 开发的开源编程语言...",
            "https://picsum.photos/800/600?random=1",
            false,
            "详细介绍 Go 语言的基础知识",
            "Go,编程,后端",
            "Go 语言入门指南"
        )

    }
}
