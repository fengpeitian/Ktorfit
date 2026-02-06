package io.github.fpt.ktorfit.sample.network.data

import kotlinx.serialization.Serializable

/**
 * @author : fengpeitian
 * e-mail  : fengpeitian@xunlei.com
 * time    : 2026/1/30 16:51
 * desc    :
 */
@Serializable
data class Sentence(
    val from: String? = null,
    val name: String? = null,
)
