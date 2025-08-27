package io.legado.app.ui.book.read

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import android.util.Log
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.read.content.ZhanweifuBookHelp
import io.legado.app.utils.GSON
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class AiSummaryDialog : DialogFragment() {

    private lateinit var binding: io.legado.app.databinding.DialogAiSummaryBinding
    private var listener: AiSummaryListener? = null

    interface AiSummaryListener {
        fun onReplace(summary: String)
    }

    fun setListener(listener: AiSummaryListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = io.legado.app.databinding.DialogAiSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val content = arguments?.getString("content")
        if (content.isNullOrEmpty()) {
            toastOnUi("本章无内容")
            dismiss()
            return
        }

        // First, try to load from the internal cache
        val book = io.legado.app.model.ReadBook.book
        val chapter = io.legado.app.model.ReadBook.curTextChapter?.chapter
        if (book != null && chapter != null) {
            val cachedSummary = ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter)
            if (cachedSummary != null) {
                binding.progressBar.visibility = View.GONE
                binding.tvSummary.visibility = View.VISIBLE
                binding.tvSummary.text = cachedSummary
                binding.btnReplaceContent.visibility = View.VISIBLE
                binding.btnReplaceContent.setOnClickListener {
                    listener?.onReplace(cachedSummary)
                    dismiss()
                }
                // We don't save here because it's already cached internally.
                // The external save is a separate action.
                return
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvSummary.visibility = View.GONE
        binding.btnReplaceContent.visibility = View.GONE

        val apiKey = AppConfig.aiSummaryApiKey
        val apiUrl = AppConfig.aiSummaryApiUrl
        val modelId = AppConfig.aiSummaryModelId ?: "gpt-3.5-turbo"
        val systemPrompt = AppConfig.aiSummarySystemPrompt ?: "请总结以下内容："

        if (apiKey.isNullOrEmpty() || apiUrl.isNullOrEmpty()) {
            toastOnUi("请先设置AI摘要的API Key和URL")
            dismiss()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val mediaType = "application/json; charset=utf-g".toMediaType()
                val messages = mutableListOf<Map<String, String>>()
                messages.add(mapOf("role" to "system", "content" to systemPrompt))
                messages.add(mapOf("role" to "user", "content" to content))
                val requestBody = GSON.toJson(mapOf(
                    "model" to modelId,
                    "messages" to messages
                )).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()

                Log.d("AiSummary", "请求 URL: $apiUrl")
                Log.d("AiSummary", "请求头: ${request.headers}")
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                Log.d("AiSummary", "请求体: ${buffer.readUtf8()}")

                val response = client.newCall(request).execute()

                Log.d("AiSummary", "响应代码: ${response.code}")
                Log.d("AiSummary", "响应消息: ${response.message}")
                Log.d("AiSummary", "响应头: ${response.headers}")

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("AiSummary", "成功响应体: $responseBody")
                    val summary = GSON.fromJson(responseBody, Map::class.java)
                        ?.get("choices")?.let { it as List<*> }?.get(0)?.let { it as Map<*, *> }?.get("message")?.let { it as Map<*, *> }?.get("content") as? String

                    if (summary != null) {
                        Log.d("AiSummary", "解析的摘要: $summary")
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.tvSummary.visibility = View.VISIBLE
                            binding.tvSummary.text = summary
                            binding.btnReplaceContent.visibility = View.VISIBLE
                            binding.btnReplaceContent.setOnClickListener {
                                listener?.onReplace(summary)
                                dismiss()
                            }
                            // Also save to internal cache for consistency
                            if (book != null && chapter != null) {
                                ZhanweifuBookHelp.saveAiSummaryToCache(book, chapter, summary)
                            }
                            saveSummaryToFile(summary)
                        }
                    } else {
                        toastOnUi("AI摘要失败：无法解析响应")
                    }
                } else {
                    val errorBody = response.body?.string()
                    Log.e("AiSummary", "错误响应代码: ${response.code}, 消息: ${response.message}, 响应体: $errorBody")
                    toastOnUi("AI摘要失败：${response.message}. 详情请查看日志")
                }
            } catch (e: IOException) {
                toastOnUi("AI摘要失败：${e.message}")
            }
        }
    }

    private fun saveSummaryToFile(summary: String) {
        val book = io.legado.app.model.ReadBook.book ?: return
        val chapter = io.legado.app.model.ReadBook.curTextChapter?.chapter ?: return

        val cachePathUriString = AppConfig.aiSummaryCachePath
        val saveFormat = AppConfig.aiSummarySaveFormat
        val saveMode = AppConfig.aiSummarySaveMode

        if (cachePathUriString.isNullOrEmpty()) {
            toastOnUi("AI摘要保存失败：请先在设置中设置缓存目录")
            return
        }

        Log.d("AiSummary", "缓存路径URI字符串: $cachePathUriString")
        Log.d("AiSummary", "保存格式: $saveFormat")
        Log.d("AiSummary", "保存模式: $saveMode")

        val fileName = "${book.name}"
        val fileExtension = if (saveFormat == "json") "json" else "txt"

        try {
            val cacheUri = Uri.parse(cachePathUriString)
            val pickedDir = try {
                DocumentFile.fromTreeUri(requireContext(), cacheUri)
            } catch (e: IllegalArgumentException) {
                Log.e("AiSummary", "格式错误的缓存URI: $cachePathUriString, 错误: ${e.message}")
                toastOnUi("AI摘要保存失败：缓存目录URI格式错误")
                return
            } catch (e: SecurityException) {
                Log.e("AiSummary", "缓存URI权限被拒绝: $cachePathUriString, 错误: ${e.message}")
                toastOnUi("AI摘要保存失败：无权限访问缓存目录，请重新选择")
                return
            }
            Log.d("AiSummary", "选择的目录: $pickedDir")
            if (pickedDir != null) {
                Log.d("AiSummary", "选择的目录存在: ${pickedDir.exists()}")
                Log.d("AiSummary", "选择的目录是文件夹: ${pickedDir.isDirectory()}")
            }

            if (pickedDir == null || !pickedDir.exists() || !pickedDir.isDirectory) {
                Log.e("AiSummary", "无效或不存在的缓存目录: $cachePathUriString")
                toastOnUi("AI摘要保存失败：缓存目录无效或不存在")
                return
            }

            // Create book-specific subdirectory
            var bookDir = pickedDir.findFile(book.name)
            if (bookDir == null || !bookDir.exists() || !bookDir.isDirectory) {
                bookDir = pickedDir.createDirectory(book.name)
                if (bookDir == null) {
                    Log.e("AiSummary", "创建书籍目录失败: ${book.name}")
                    toastOnUi("AI摘要保存失败：无法创建书籍目录")
                    return
                }
            }

            when (saveMode) {
                "overwrite", "append" -> {
                    var targetFile = bookDir.findFile("$fileName.$fileExtension")
                    if (targetFile == null) {
                        targetFile = bookDir.createFile("text/${fileExtension}", "$fileName.$fileExtension")
                    }

                    if (targetFile == null) {
                        Log.e("AiSummary", "创建或查找文件失败: $fileName.$fileExtension")
                        toastOnUi("AI摘要保存失败：无法创建或找到文件")
                        return
                    }

                    if (saveMode == "overwrite") {
                        requireContext().contentResolver.openOutputStream(targetFile.uri)?.use { outputStream: OutputStream ->
                            outputStream.write(summary.toByteArray())
                        }
                        Log.d("AiSummary", "摘要已保存 (覆盖): ${targetFile.uri}")
                        toastOnUi("AI摘要保存成功")
                    } else { // append
                        requireContext().contentResolver.openOutputStream(targetFile.uri, "wa")?.use { outputStream: OutputStream ->
                            outputStream.write("""

--- ${chapter.title} ---

""".toByteArray(Charsets.UTF_8))
                            outputStream.write(summary.toByteArray())
                        }
                        Log.d("AiSummary", "摘要已保存 (追加): ${targetFile.uri}")
                        toastOnUi("AI摘要保存成功")
                    }
                }
                "new_file" -> {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val newFileName = "${fileName}_${timestamp}.$fileExtension"
                    val newFile = bookDir.createFile("text/${fileExtension}", newFileName)
                    if (newFile != null) {
                        requireContext().contentResolver.openOutputStream(newFile.uri)?.use { outputStream: OutputStream ->
                            outputStream.write(summary.toByteArray())
                        }
                        Log.d("AiSummary", "摘要已保存 (新文件): ${newFile.uri}")
                        toastOnUi(getString(R.string.summary_save_success, newFile.uri.path))
                    } else {
                        Log.e("AiSummary", "创建新文件失败: $newFileName")
                        toastOnUi(getString(R.string.summary_save_failed, "无法创建新文件"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AiSummary", "将摘要保存到文件时出错: ${e.message}")
            toastOnUi("AI摘要保存失败：${e.message}")
        }
    }
}