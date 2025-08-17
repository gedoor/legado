package io.legado.app.ui.book.read.content

import com.google.gson.stream.JsonReader
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import splitties.init.appCtx
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ZhanweifuBookHelp {

    private const val cacheFolderName = "zhanweifu_cache"
    private val cacheDir: File by lazy { appCtx.externalFiles.getFile(cacheFolderName) }

    fun zhanweifuGetContent(book: Book, bookChapter: BookChapter): String? {
        val file = cacheDir.getFile(book.getFolderName(), bookChapter.getFileName())
        if (file.exists()) {
            return file.readText()
        }
        return null
    }

    fun zhanweifuSaveText(book: Book, bookChapter: BookChapter, content: String) {
        cacheDir.getFile(book.getFolderName(), bookChapter.getFileName())
            .createFileIfNotExist().writeText(content)
    }

    fun zhanweifuDelContent(book: Book, bookChapter: BookChapter) {
        val file = cacheDir.getFile(book.getFolderName(), bookChapter.getFileName())
        if (file.exists()) {
            file.delete()
        }
    }

    fun getAiSummaryFromCache(book: Book, chapter: BookChapter): String? {
        val file = cacheDir.getFile(book.getFolderName(), chapter.getFileName() + "_ai_summary")
        return if (file.exists()) file.readText() else null
    }

    fun saveAiSummaryToCache(book: Book, chapter: BookChapter, summary: String) {
        val file = cacheDir.getFile(book.getFolderName(), chapter.getFileName() + "_ai_summary")
        file.createFileIfNotExist().writeText(summary)
    }

    suspend fun getAiSummary(
        content: String,
        onResponse: (String) -> Unit,
        onFinish: () -> Unit,
        onError: (String) -> Unit
    ) {
        val apiKey = AppConfig.aiSummaryApiKey
        val apiUrl = AppConfig.aiSummaryApiUrl
        if (apiKey.isNullOrEmpty() || apiUrl.isNullOrEmpty()) {
            onError.invoke("请先设置AI摘要的API Key和URL")
            onFinish.invoke()
            return
        }

        
        val wordCount = content.length
        LogUtils.d("AiSummary", "开始生成AI摘要，请求字数：${wordCount}")
        // Log the full content in chunks to avoid truncation
        val chunkSize = 3000
        for (i in 0..content.length / chunkSize) {
            val start = i * chunkSize
            var end = (i + 1) * chunkSize
            if (end > content.length) {
                end = content.length
            }
            LogUtils.d("AiSummary_Content", content.substring(start, end))
        }
        val newContent = "本章${wordCount}字，下面是正文内容：\n\n${content}"

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to (AppConfig.aiSummarySystemPrompt ?: "请总结以下内容：")))
        LogUtils.d("AiSummary", (AppConfig.aiSummarySystemPrompt ?: "请总结以下内容："))
        messages.add(mapOf("role" to "user", "content" to newContent))
        val requestBody = GSON.toJson(mapOf(
            "model" to (AppConfig.aiSummaryModelId ?: "gpt-3.5-turbo"),
            "messages" to messages,
            "stream" to true
        )).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "text/event-stream")
            .build()

        try {
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use {
                    if (!it.isSuccessful) {
                        throw IOException("Unexpected code $it")
                    }
                    handleStreamResponse(it, onResponse, onFinish, onError)
                }
            }
        } catch (e: IOException) {
            LogUtils.e("getAiSummary", e.stackTraceToString())
            withContext(Dispatchers.Main) {
                onError.invoke("请求失败: ${e.message}")
                onFinish.invoke()
            }
        }
    }

    private suspend fun handleStreamResponse(
        response: Response,
        onResponse: (String) -> Unit,
        onFinish: () -> Unit,
        onError: (String) -> Unit
    ) {
        val source = response.body?.source() ?: return
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data:")) {
                    val data = line.substring(5).trim()
                    if (data == "[DONE]") {
                        break
                    }
                    try {
                        val reader = JsonReader(data.reader())
                        val chunk = GSON.fromJson<Map<String, Any>>(reader, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
                        val choices = chunk["choices"] as? List<*>
                        val delta = choices?.firstOrNull() as? Map<*, *>
                        val content = delta?.get("delta") as? Map<*, *>
                        val text = content?.get("content") as? String
                        if (!text.isNullOrEmpty()) {
                            withContext(Dispatchers.Main) {
                                onResponse.invoke(text)
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for now
                    }
                }
            }
        } catch (e: IOException) {
            LogUtils.e("handleStreamResponse", e.stackTraceToString())
            withContext(Dispatchers.Main) {
                onError.invoke("读取数据流失败: ${e.message}")
            }
        } finally {
            withContext(Dispatchers.Main) {
                onFinish.invoke()
            }
        }
    }
}
