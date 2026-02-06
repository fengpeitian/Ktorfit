package io.github.fpt.ktorfit.sample.network.data

import kotlinx.serialization.Serializable

/**
 * @author : fengpeitian
 * e-mail  : fengpeitian@xunlei.com
 * time    : 2026/1/30 16:16
 * desc    :
 */
@Serializable
class Result<T> {

    val code: Int = 0

    val message: String? = null

    val data: T? = null

}