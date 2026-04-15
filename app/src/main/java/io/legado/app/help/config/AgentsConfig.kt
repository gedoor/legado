package io.legado.app.help.config

import io.legado.app.constant.PreferKey
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import splitties.init.appCtx
import java.net.URL

/**
 * Agents 配置统一读取/写入入口。
 *
 * 说明：
 * - 本对象负责封装 Agents 相关 SharedPreferences 的 key、默认值与校验规则。
 * - UI 层（Preference 页）应尽量通过本对象进行读写，避免在业务侧分散直接触碰偏好存储。
 */
@Suppress("MemberVisibilityCanBePrivate")
object AgentsConfig {

    /** 默认请求超时（秒）。 */
    const val DEFAULT_REQUEST_TIMEOUT_SEC: Int = 60

    /** 默认 temperature。 */
    const val DEFAULT_TEMPERATURE: Double = 1.0

    /** 默认最大输出 token 数。 */
    const val DEFAULT_MAX_OUTPUT_TOKENS: Int = 4096

    /** 默认历史查询上限。 */
    const val DEFAULT_HISTORY_LIMIT: Int = 200

    /**
     * 获取 Base URL。
     *
     * Returns:
     *     String: 当前配置的 Base URL（可能为空字符串）。
     */
    fun getBaseUrl(): String = appCtx.getPrefString(PreferKey.agentsBaseUrl, "") ?: ""

    /**
     * 尝试写入 Base URL（允许空；非空时必须为 http/https 且可被 URL 解析）。
     *
     * Args:
     *     raw (String?): 用户输入的原始 Base URL。
     *
     * Returns:
     *     Boolean: 是否写入成功。
     */
    fun trySetBaseUrl(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            appCtx.removePref(PreferKey.agentsBaseUrl)
            return true
        }
        if (!isValidHttpUrl(value)) {
            return false
        }
        appCtx.putPrefString(PreferKey.agentsBaseUrl, value)
        return true
    }

    /**
     * 获取 API Key。
     *
     * Returns:
     *     String: 当前配置的 API Key（可能为空字符串）。
     */
    fun getApiKey(): String = appCtx.getPrefString(PreferKey.agentsApiKey, "") ?: ""

    /**
     * 写入 API Key（允许空；空时会删除偏好项）。
     *
     * Args:
     *     raw (String?): 用户输入的原始 API Key。
     *
     * Returns:
     *     Boolean: 是否写入成功（当前始终为 true）。
     */
    fun trySetApiKey(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            appCtx.removePref(PreferKey.agentsApiKey)
        } else {
            appCtx.putPrefString(PreferKey.agentsApiKey, value)
        }
        return true
    }

    /**
     * 获取模型名称。
     *
     * Returns:
     *     String: 当前配置的模型名称（可能为空字符串）。
     */
    fun getModel(): String = appCtx.getPrefString(PreferKey.agentsModel, "") ?: ""

    /**
     * 写入模型名称（允许空；空时会删除偏好项）。
     *
     * Args:
     *     raw (String?): 用户输入的原始模型名称。
     *
     * Returns:
     *     Boolean: 是否写入成功（当前始终为 true）。
     */
    fun trySetModel(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            appCtx.removePref(PreferKey.agentsModel)
        } else {
            appCtx.putPrefString(PreferKey.agentsModel, value)
        }
        return true
    }

    /**
     * 获取请求超时（秒）。
     *
     * Returns:
     *     Int: 超时秒数。
     */
    fun getRequestTimeoutSec(): Int =
        appCtx.getPrefInt(PreferKey.agentsRequestTimeout, DEFAULT_REQUEST_TIMEOUT_SEC)
            .let { value ->
                // 兜底处理：偏好可能来自旧版本/导入，确保返回值始终在允许范围内。
                if (value in 5..600) value else DEFAULT_REQUEST_TIMEOUT_SEC
            }

    /**
     * 尝试写入请求超时（秒）。
     *
     * Args:
     *     value (Int): 超时秒数。
     *
     * Returns:
     *     Boolean: 是否写入成功。
     */
    fun trySetRequestTimeoutSec(value: Int): Boolean {
        if (value !in 5..600) return false
        appCtx.putPrefInt(PreferKey.agentsRequestTimeout, value)
        return true
    }

    /**
     * 获取 temperature。
     *
     * Returns:
     *     Double: temperature。
     */
    fun getTemperature(): Double {
        val raw = appCtx.getPrefString(PreferKey.agentsTemperature, null)
        val value = raw?.toDoubleOrNull() ?: DEFAULT_TEMPERATURE
        return value.coerceIn(0.0, 2.0)
    }

    /**
     * 尝试写入 temperature。
     *
     * Args:
     *     value (Double): temperature。
     *
     * Returns:
     *     Boolean: 是否写入成功。
     */
    fun trySetTemperature(value: Double): Boolean {
        if (value < 0.0 || value > 2.0) return false
        // SharedPreferences 不直接支持 Double，这里使用字符串持久化。
        appCtx.putPrefString(PreferKey.agentsTemperature, value.toString())
        return true
    }

    /**
     * 获取最大输出 token 数。
     *
     * Returns:
     *     Int: 最大输出 token 数。
     */
    fun getMaxOutputTokens(): Int =
        appCtx.getPrefInt(PreferKey.agentsMaxOutputTokens, DEFAULT_MAX_OUTPUT_TOKENS)
            .let { value ->
                // 兜底处理：偏好可能来自旧版本/导入，确保返回值始终在允许范围内。
                if (value in 256..32768) value else DEFAULT_MAX_OUTPUT_TOKENS
            }

    /**
     * 尝试写入最大输出 token 数。
     *
     * Args:
     *     value (Int): token 数。
     *
     * Returns:
     *     Boolean: 是否写入成功。
     */
    fun trySetMaxOutputTokens(value: Int): Boolean {
        if (value !in 256..32768) return false
        appCtx.putPrefInt(PreferKey.agentsMaxOutputTokens, value)
        return true
    }

    /**
     * 获取自定义提示词。
     *
     * Returns:
     *     String: 自定义提示词（可能为空字符串）。
     */
    fun getCustomInstruction(): String =
        appCtx.getPrefString(PreferKey.agentsCustomInstruction, "") ?: ""

    /**
     * 写入自定义提示词（允许空；空时会删除偏好项）。
     *
     * Args:
     *     raw (String?): 用户输入的提示词。
     *
     * Returns:
     *     Boolean: 是否写入成功（当前始终为 true）。
     */
    fun trySetCustomInstruction(raw: String?): Boolean {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            appCtx.removePref(PreferKey.agentsCustomInstruction)
        } else {
            // 注意：这里保留换行与内容，但去掉首尾空白，避免 UI 输入的意外空格影响请求。
            appCtx.putPrefString(PreferKey.agentsCustomInstruction, value)
        }
        return true
    }

    /**
     * 是否启用书架工具。
     */
    var enableBookshelfTools: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.agentsEnableBookshelfTools, true)
        set(value) = appCtx.putPrefBoolean(PreferKey.agentsEnableBookshelfTools, value)

    /**
     * 是否启用已加载章节工具。
     */
    var enableLoadedChapterTools: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.agentsEnableLoadedChapterTools, true)
        set(value) = appCtx.putPrefBoolean(PreferKey.agentsEnableLoadedChapterTools, value)

    /**
     * 是否启用操作规则工具。
     */
    var enableOperationRuleTools: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.agentsEnableOperationRuleTools, true)
        set(value) = appCtx.putPrefBoolean(PreferKey.agentsEnableOperationRuleTools, value)

    /**
     * 是否自动生成会话标题。
     */
    var autoCreateTitle: Boolean
        get() = appCtx.getPrefBoolean(PreferKey.agentsAutoCreateTitle, true)
        set(value) = appCtx.putPrefBoolean(PreferKey.agentsAutoCreateTitle, value)

    /**
     * 获取历史查询上限。
     */
    fun getHistoryLimit(): Int =
        appCtx.getPrefInt(PreferKey.agentsHistoryLimit, DEFAULT_HISTORY_LIMIT)
            .let { value ->
                // 兜底处理：偏好可能来自旧版本/导入，确保返回值始终在允许范围内。
                if (value in 20..1000) value else DEFAULT_HISTORY_LIMIT
            }

    /**
     * 尝试写入历史查询上限。
     *
     * Args:
     *     value (Int): 历史上限。
     *
     * Returns:
     *     Boolean: 是否写入成功。
     */
    fun trySetHistoryLimit(value: Int): Boolean {
        if (value !in 20..1000) return false
        appCtx.putPrefInt(PreferKey.agentsHistoryLimit, value)
        return true
    }

    /**
     * 判断字符串是否为合法的 http/https URL。
     *
     * Args:
     *     value (String): 待校验的 URL 字符串。
     *
     * Returns:
     *     Boolean: 是否合法。
     */
    fun isValidHttpUrl(value: String): Boolean {
        return runCatching {
            val url = URL(value)
            val protocolOk = url.protocol.equals("http", true) || url.protocol.equals("https", true)
            // URL("http://") 这种场景 host 可能为空；这里做一次兜底。
            protocolOk && url.host.isNotBlank()
        }.getOrDefault(false)
    }
}
