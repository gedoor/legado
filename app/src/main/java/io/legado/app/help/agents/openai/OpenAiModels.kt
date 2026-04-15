package io.legado.app.help.agents.openai

import androidx.annotation.Keep
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * /v1/models 响应体（最小字段集合）。
 */
@Keep
data class OpenAiListModelsResponse(
    /**
     * 模型列表。
     */
    val data: List<OpenAiModel>? = null,
)

/**
 * OpenAI 模型信息（最小字段集合）。
 */
@Keep
data class OpenAiModel(
    /**
     * 模型 ID，例如：gpt-4o-mini。
     */
    val id: String,
)

/**
 * /v1/chat/completions 请求体（最小能力）。
 *
 * 注意：不同的 OpenAI 兼容实现对“最大输出 token”字段命名可能不一致：
 * - 经典 Chat Completions：max_tokens
 * - 部分兼容实现/网关：max_output_tokens
 *
 * 为了兼容性，本模型同时提供两个字段；但 **禁止同时设置**，否则可能导致服务端参数冲突。
 */
@Keep
data class OpenAiChatCompletionsRequest(
    /**
     * 目标模型 ID。
     */
    val model: String,

    /**
     * 对话消息列表。
     */
    val messages: List<OpenAiChatMessage>,

    /**
     * 采样温度，越大输出越随机。
     */
    val temperature: Double? = null,

    /**
     * 最大输出 token 数（兼容字段名：max_output_tokens）。
     *
     * 说明：若你的服务端仅支持 max_tokens，请使用 [maxTokens]。
     */
    @SerializedName("max_output_tokens")
    val maxOutputTokens: Int? = null,

    /**
     * 最大输出 token 数（经典字段名：max_tokens）。
     *
     * 说明：若你的服务端支持 max_output_tokens，优先使用 [maxOutputTokens]。
     */
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,

    /**
     * 可选工具定义（function tools）。
     */
    val tools: List<OpenAiTool>? = null,
) {

    init {
        // 避免同时序列化两个“最大输出 token”字段，导致服务端出现参数冲突或优先级不确定。
        require(!(maxOutputTokens != null && maxTokens != null)) {
            "maxOutputTokens 与 maxTokens 不能同时设置"
        }
    }
}

/**
 * Chat 消息。
 */
@Keep
data class OpenAiChatMessage(
    /**
     * 角色：system/user/assistant/tool。
     */
    val role: String,

    /**
     * 文本内容。
     *
     * 说明：本子任务仅要求最小文本 content；多模态/数组 content 不在范围内。
     */
    val content: String? = null,

    /**
     * 可选名称字段（某些兼容实现会用到）。
     */
    val name: String? = null,
)

/**
 * 工具定义（function tool）。
 */
@Keep
data class OpenAiTool(
    /**
     * 工具类型：OpenAI function tool 固定为 "function"。
     */
    val type: String = "function",

    /**
     * function 描述。
     */
    val function: OpenAiFunctionTool,
)

/**
 * function tool 的定义。
 */
@Keep
data class OpenAiFunctionTool(
    /**
     * 函数名。
     */
    val name: String,

    /**
     * 函数描述。
     */
    val description: String? = null,

    /**
     * JSON Schema 参数定义。
     *
     * 说明：使用 JsonElement 保留 schema 的原始结构，避免在此层强绑定某种参数类型。
     */
    val parameters: JsonElement? = null,
)

/**
 * /v1/chat/completions 响应体（最小字段集合）。
 */
@Keep
data class OpenAiChatCompletionsResponse(
    val choices: List<OpenAiChatChoice>? = null,
)

/**
 * Chat Completions 的 choice。
 */
@Keep
data class OpenAiChatChoice(
    val message: OpenAiAssistantMessage? = null,
)

/**
 * 助手消息（包含文本与 tool_calls）。
 */
@Keep
data class OpenAiAssistantMessage(
    val content: String? = null,

    /**
     * 工具调用（OpenAI 字段名：tool_calls）。
     */
    @SerializedName("tool_calls")
    val toolCalls: List<OpenAiToolCall>? = null,
)

/**
 * 工具调用项（最小字段集合）。
 */
@Keep
data class OpenAiToolCall(
    val id: String? = null,
    val type: String? = null,
    val function: OpenAiToolCallFunction? = null,
)

/**
 * 工具调用 function 部分（最小字段集合）。
 */
@Keep
data class OpenAiToolCallFunction(
    val name: String? = null,

    /**
     * function arguments（OpenAI 规范为 JSON 字符串）。
     */
    val arguments: String? = null,
)

/**
 * 业务层更易用的 Chat Completions 解析结果。
 */
@Keep
data class OpenAiChatParsedResult(
    /**
     * choices[0].message.content 的文本内容。
     */
    val content: String? = null,

    /**
     * choices[0].message.tool_calls（若存在）。
     */
    val toolCalls: List<OpenAiToolCall>? = null,
)

/**
 * OpenAI 错误响应（最小字段集合）。
 */
@Keep
data class OpenAiErrorResponse(
    val error: OpenAiErrorDetail? = null,
)

/**
 * OpenAI error 细节（最小字段集合）。
 */
@Keep
data class OpenAiErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)
