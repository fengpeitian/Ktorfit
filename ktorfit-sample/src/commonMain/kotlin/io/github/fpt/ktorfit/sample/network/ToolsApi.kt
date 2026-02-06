package io.github.fpt.ktorfit.sample.network

import io.github.fpt.ktorfit.annotations.Body
import io.github.fpt.ktorfit.annotations.GET
import io.github.fpt.ktorfit.annotations.POST
import io.github.fpt.ktorfit.sample.network.data.Article
import io.github.fpt.ktorfit.sample.network.data.ArticleResp
import io.github.fpt.ktorfit.sample.network.data.Sentence
import io.github.fpt.ktorfit.sample.network.data.Result

/**
 * @author : fengpeitian
 * e-mail  : fengpeitian@xunlei.com
 * time    : 2026/2/5 17:21
 * desc    :
 */
interface ToolsApi {

    @POST("/api/articles")
    suspend fun publishArticle(@Body article: Article): Result<ArticleResp?>

    @GET("/api/tools/famous-sentence")
    suspend fun getSentence(): Result<Sentence?>

}