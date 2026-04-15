package io.legado.app.help.agents.openai

import com.google.gson.JsonSyntaxException
import io.legado.app.help.http.await
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postJson
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI 兼容协议客户端（最小可用）。
 *
 * 支持：
 * 1) GET /v1/models
 * 2) POST /v1/chat/completions
 *
 * 设计约束：
 * - 复用项目现有 OkHttp/Gson 基建（HttpHelper/OkHttpUtils/GSON）；
 * - 统一输出结构化错误，避免直接抛异常导致崩溃；
 * - 不记录、不返回敏感信息（例如 API Key）。
 *
 * @param baseUrl OpenAI 兼容服务的 Base URL（带或不带尾部 / 均可）。
 * @param apiKey API Key（会以 Authorization: Bearer 形式发送，禁止写入日志）。
 * @param timeoutSeconds 超时时间（秒），用于 connect/write/read/call 的统一超时。
 * @param httpClient 可注入 OkHttpClient，默认复用全局 okHttpClient 并按超时参数派生。
 */
class OpenAiClient(
    baseUrl: String,
    private val apiKey: String,
    timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    httpClient: OkHttpClient = okHttpClient,
) {

    init {
        // 提前校验关键参数，避免在实际请求时才以更隐晦的方式失败。
        require(apiKey.isNotBlank()) { "apiKey 不能为空" }
    }

    /**
     * 规范化后的 baseUrl：
     * - 去除首尾空白
     * - 去除尾部多余的 '/'
     */
    private val normalizedBaseUrl: String = normalizeBaseUrl(baseUrl)

    /**
     * 用于实际请求的 OkHttpClient。
     *
     * 说明：
     * - 在全局 okHttpClient 的基础上派生，复用其拦截器、TLS、代理等既有约定；
     * - 仅按本客户端配置覆盖超时，避免影响全局。
     */
    private val client: OkHttpClient = httpClient
        .newBuilder()
        .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .callTimeout(timeoutSeconds, TimeUnit.SECONDS)
        .build()

    /**
     * 获取模型列表（连通性测试）。
     *
     * @return 成功时返回模型列表；失败时返回结构化错误。
     */
    suspend fun listModels(): OpenAiClientResult<List<OpenAiModel>> {
        val url = endpoint("/v1/models")
        return executeGet(url) { body ->
            val parsed = parseJson<OpenAiListModelsResponse>(body)
            OpenAiClientResult.Success(parsed.data.orEmpty())
        }
    }

    /**
     * 调用 /v1/chat/completions（最小能力）。
     *
     * 解析：
     * - 提取 choices[0].message.content
     * - 若存在 tool_calls，则一并解析并返回给上层（不在此处执行工具循环）
     *
     * @param request Chat Completions 请求体。
     * @return 成功时返回解析结果；失败时返回结构化错误。
     */
    suspend fun chatCompletions(
        request: OpenAiChatCompletionsRequest,
    ): OpenAiClientResult<OpenAiChatParsedResult> {
        val url = endpoint("/v1/chat/completions")
        val json = GSON.toJson(request)
        return executePostJson(url, json) { body ->
            val parsed = parseJson<OpenAiChatCompletionsResponse>(body)
            val firstMessage = parsed.choices.orEmpty().firstOrNull()?.message
            if (firstMessage == null) {
                return@executePostJson OpenAiClientResult.Failure(
                    OpenAiClientError(
                        message = "响应缺少 choices.message 字段",
                        httpCode = null,
                        retryable = false,
                    )
                )
            }
            OpenAiClientResult.Success(
                OpenAiChatParsedResult(
                    content = firstMessage.content,
                    toolCalls = firstMessage.toolCalls,
                )
            )
        }
    }

    /**
     * 执行 GET 请求并按统一规则处理错误与解析。
     *
     * @param url 完整 URL。
     * @param onSuccessBody 当 HTTP 2xx 且 body 非空时执行的解析逻辑。
     * @return 统一结果类型。
     */
    private suspend fun <T> executeGet(
        url: String,
        onSuccessBody: (String) -> OpenAiClientResult<T>,
    ): OpenAiClientResult<T> {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")

        return executeRequest(requestBuilder, onSuccessBody)
    }

    /**
     * 执行 JSON POST 请求并按统一规则处理错误与解析。
     *
     * @param url 完整 URL。
     * @param jsonBody JSON 字符串请求体。
     * @param onSuccessBody 当 HTTP 2xx 且 body 非空时执行的解析逻辑。
     * @return 统一结果类型。
     */
    private suspend fun <T> executePostJson(
        url: String,
        jsonBody: String,
        onSuccessBody: (String) -> OpenAiClientResult<T>,
    ): OpenAiClientResult<T> {
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            // 这里使用项目已有的扩展方法，确保 Content-Type 与编码一致。
            .apply { postJson(jsonBody) }

        return executeRequest(requestBuilder, onSuccessBody)
    }

    /**
     * 统一执行 HTTP 请求并处理：
     * - 网络异常
     * - HTTP 非 2xx
     * - 空 body
     * - JSON 解析失败
     *
     * @param requestBuilder OkHttp 请求构造器。
     * @param onSuccessBody HTTP 2xx 且 body 非空时的解析逻辑。
     * @return 统一结果类型。
     */
    private suspend fun <T> executeRequest(
        requestBuilder: Request.Builder,
        onSuccessBody: (String) -> OpenAiClientResult<T>,
    ): OpenAiClientResult<T> {
        return try {
            // 直接使用 OkHttpUtils 的 await 扩展，避免在此重复实现协程封装。
            val response = client.newCall(requestBuilder.build()).await()
            response.use { res ->
                val httpCode = res.code
                // OkHttp 5 的 Response.body 为非空；使用 text() 统一读取并消费 body。
                val bodyStr = res.body.text()

                if (!res.isSuccessful) {
                    return OpenAiClientResult.Failure(
                        buildHttpError(httpCode = httpCode, responseBody = bodyStr)
                    )
                }

                if (bodyStr.isBlank()) {
                    return OpenAiClientResult.Failure(
                        OpenAiClientError(
                            message = "响应内容为空",
                            httpCode = httpCode,
                            retryable = false,
                        )
                    )
                }

                // 在拿到 httpCode 之后再做 JSON 解析，这样解析失败时也能带上状态码，便于上层判断。
                try {
                    onSuccessBody(bodyStr)
                } catch (e: CancellationException) {
                    // 协程取消必须向上抛出，避免被误包装成“业务失败”。
                    throw e
                } catch (e: JsonSyntaxException) {
                    // JSON 解析失败通常不可通过重试解决（更可能是服务端返回结构变化或兼容实现差异）。
                    OpenAiClientResult.Failure(
                        OpenAiClientError(
                            message = "解析响应失败: ${e.localizedMessage ?: "JSON 格式错误"}",
                            httpCode = httpCode,
                            retryable = false,
                        )
                    )
                } catch (e: Exception) {
                    // 兜底：避免任何未预期异常导致崩溃；同时保留 httpCode 便于排查。
                    OpenAiClientResult.Failure(
                        OpenAiClientError(
                            message = e.localizedMessage ?: "未知错误",
                            httpCode = httpCode,
                            retryable = null,
                        )
                    )
                }
            }
        } catch (e: IOException) {
            // OkHttpExceptionInterceptor 会把非 IOException 的 Throwable 包装为 IOException。
            OpenAiClientResult.Failure(
                OpenAiClientError(
                    message = e.localizedMessage ?: "网络异常",
                    httpCode = null,
                    retryable = true,
                )
            )
        } catch (e: CancellationException) {
            // 协程取消必须向上抛出，避免吞掉取消信号导致上层逻辑卡住。
            throw e
        } catch (e: JsonSyntaxException) {
            // JSON 解析失败通常不可通过重试解决（更可能是服务端返回结构变化或兼容实现差异）。
            OpenAiClientResult.Failure(
                OpenAiClientError(
                    message = "解析响应失败: ${e.localizedMessage ?: "JSON 格式错误"}",
                    httpCode = null,
                    retryable = false,
                )
            )
        } catch (e: Exception) {
            // 兜底：避免任何未预期异常导致崩溃。
            OpenAiClientResult.Failure(
                OpenAiClientError(
                    message = e.localizedMessage ?: "未知错误",
                    httpCode = null,
                    retryable = null,
                )
            )
        }
    }

    /**
     * JSON 解析工具：
     * - 统一把解析失败转换为 [OpenAiClientResult.Failure]
     * - 避免在调用处散落 try/catch
     *
     * @param json JSON 字符串。
     * @return 解析后的对象。
     * @throws JsonSyntaxException 当 JSON 格式不合法或字段不匹配时抛出。
     */
    private inline fun <reified T> parseJson(json: String): T {
        return GSON.fromJsonObject<T>(json)
            .getOrElse { throw JsonSyntaxException(it.localizedMessage ?: "JSON 解析失败") }
    }

    /**
     * 构造 HTTP 错误信息，优先从 OpenAI error.message 中提取可读原因。
     *
     * @param httpCode HTTP 状态码。
     * @param responseBody 响应 body（可能为空）。
     * @return 结构化错误对象。
     */
    private fun buildHttpError(httpCode: Int, responseBody: String?): OpenAiClientError {
        val parsedMessage = responseBody
            ?.takeIf { it.isNotBlank() }
            ?.let { body ->
                // 仅尝试解析标准 OpenAI error 结构；失败则降级为通用提示。
                runCatching { GSON.fromJsonObject<OpenAiErrorResponse>(body).getOrNull() }
                    .getOrNull()
                    ?.error
                    ?.message
            }

        val message = when {
            !parsedMessage.isNullOrBlank() -> parsedMessage
            else -> "请求失败($httpCode)"
        }

        return OpenAiClientError(
            message = message,
            httpCode = httpCode,
            retryable = isRetryableHttpCode(httpCode),
        )
    }

    /**
     * 根据 HTTP 状态码判断是否建议重试。
     *
     * @param httpCode HTTP 状态码。
     * @return true 表示通常可重试；false 表示通常不可重试。
     */
    private fun isRetryableHttpCode(httpCode: Int): Boolean {
        return httpCode == 429 || httpCode in 500..599
    }

    /**
     * 拼接 endpoint URL，确保 baseUrl 尾部斜杠兼容。
     *
     * @param path 以 / 开头的路径，例如 /v1/models。
     * @return 拼接后的完整 URL。
     */
    private fun endpoint(path: String): String {
        // 这里约束调用方传入以 '/' 开头的 path，避免出现 double slash 或缺失 '/' 的边界问题。
        require(path.startsWith("/")) { "path 必须以 '/' 开头: $path" }
        return normalizedBaseUrl + path
    }

    companion object {

        /**
         * 默认超时时间（秒）。
         */
        const val DEFAULT_TIMEOUT_SECONDS: Long = 60

        /**
         * 规范化 baseUrl：
         * - 去除首尾空白
         * - 移除尾部 '/'（重复的也一并移除）
         * - 校验 URL 形态，尽早在构造阶段失败（避免请求时才抛异常）
         *
         * @param baseUrl 原始 baseUrl。
         * @return 规范化后的 baseUrl。
         * @throws IllegalArgumentException 当 baseUrl 为空或不是合法 URL 时抛出。
         */
        fun normalizeBaseUrl(baseUrl: String): String {
            val trimmed = baseUrl.trim()
            require(trimmed.isNotBlank()) { "baseUrl 不能为空" }

            // 移除尾部多余的 '/'
            val noTrailingSlash = trimmed.trimEnd { it == '/' }

            require(noTrailingSlash.toHttpUrlOrNull() != null) { "baseUrl 不是合法 URL: $baseUrl" }
            return noTrailingSlash
        }
    }
}
