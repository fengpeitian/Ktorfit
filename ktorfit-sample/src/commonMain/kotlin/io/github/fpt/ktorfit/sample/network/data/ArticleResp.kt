package io.github.fpt.ktorfit.sample.network.data

import kotlinx.serialization.Serializable

/**
 * @author : fengpeitian
 * e-mail  : fengpeitian@xunlei.com
 * time    : 2026/2/5 17:33
 * desc    :
 */
@Serializable
data class ArticleResp(
    val category: String? = null,
    val content: String? = null,
    val cover_image: String? = null,
    val created_at: String? = null,
    val id: Int = 0,
    val is_pinned: Boolean = false,
    val is_published: Boolean = false,
    val like_count: Int = 0,
    val summary: String? = null,
    val tags: String? = null,
    val title: String? = null,
    val updated_at: String? = null,
    val view_count: Int = 0,
    val author: Author? = null
) {

    @Serializable
    data class Author(
        val account_id: Int = 0,
        val age: Int = 0,
        val avatar: String? = null,
        val bio: String? = null,
        val created_at: String? = null,
        val email: String? = null,
        val gender: String? = null,
        val location: String? = null,
        val nickname: String? = null,
        val updated_at: String? = null,
    )

}