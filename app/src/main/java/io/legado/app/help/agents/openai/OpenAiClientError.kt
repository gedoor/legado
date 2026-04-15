package io.legado.app.help.agents.openai

import androidx.annotation.Keep

/**
 * OpenAI 兼容协议客户端的结构化错误。
 *
 * 设计目标：
 * 1) 将网络异常、HTTP 非 2xx、JSON 解析失败统一封装为“可读且可判断”的错误对象；
 * 2) 让上层可以根据 [httpCode] 与 [retryable] 决定是否提示用户/是否重试；
 * 3) 避免泄露敏感信息（例如 API Key、鉴权 Header 等）。
 *
 * @property message 面向用户/上层的可读错误信息（不包含敏感信息）。
 * @property httpCode HTTP 状态码（仅当服务端有返回时可用）。
 * @property retryable 是否建议重试：
 *   - true：通常为网络波动、429/5xx 等短暂性错误；
 *   - false：通常为 400/401/403 等参数/鉴权错误；
 *   - null：无法判断（上层可自定策略）。
 */
@Keep
data class OpenAiClientError(
    val message: String,
    val httpCode: Int? = null,
    val retryable: Boolean? = null,
)

/**
 * OpenAI 客户端调用结果。
 *
 * 说明：
 * - 该结果类型用于避免在调用链上直接抛异常导致崩溃；
 * - 若业务更偏向异常流，也可以在上层将 [Failure] 转换为异常抛出。
 */
@Keep
sealed class OpenAiClientResult<out T> {

    /**
     * 调用成功。
     *
     * @property value 成功返回的数据。
     */
    @Keep
    data class Success<T>(val value: T) : OpenAiClientResult<T>()

    /**
     * 调用失败。
     *
     * @property error 结构化错误对象。
     */
    @Keep
    data class Failure(val error: OpenAiClientError) : OpenAiClientResult<Nothing>()
}
