package io.legado.app.ui.book.read

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.config.AiSummaryConfigFragment
import io.legado.app.utils.GSON
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import androidx.documentfile.provider.DocumentFile
import android.net.Uri
import splitties.init.appCtx
import java.io.OutputStream

class AiSummaryDialog : DialogFragment() {

    private lateinit var binding: io.legado.app.databinding.DialogAiSummaryBinding

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

        binding.progressBar.visibility = View.VISIBLE
        binding.tvSummary.visibility = View.GONE

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
                val client = OkHttpClient()
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

                io.legado.app.utils.LogUtils.d("AiSummary", "Request URL: $apiUrl")
                io.legado.app.utils.LogUtils.d("AiSummary", "Request Headers: ${request.headers}")
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                io.legado.app.utils.LogUtils.d("AiSummary", "Request Body: ${buffer.readUtf8()}")

                val response = client.newCall(request).execute()

                io.legado.app.utils.LogUtils.d("AiSummary", "Response Code: ${response.code}")
                io.legado.app.utils.LogUtils.d("AiSummary", "Response Message: ${response.message}")
                io.legado.app.utils.LogUtils.d("AiSummary", "Response Headers: ${response.headers}")

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    io.legado.app.utils.LogUtils.d("AiSummary", "Successful Response Body: $responseBody")
                    val summary = GSON.fromJson(responseBody, Map::class.java)
                        ?.get("choices")?.let { it as List<*> }?.get(0)?.let { it as Map<*, *> }?.get("message")?.let { it as Map<*, *> }?.get("content") as? String

                    if (summary != null) {
                        io.legado.app.utils.LogUtils.d("AiSummary", "Parsed Summary: $summary")
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            binding.tvSummary.visibility = View.VISIBLE
                            binding.tvSummary.text = summary
                            saveSummaryToFile(summary)
                        }
                    } else {
                        toastOnUi("AI摘要失败：无法解析响应")
                    }
                } else {
                    val errorBody = response.body?.string()
                    io.legado.app.utils.LogUtils.e("AiSummary", "Error Response Code: ${response.code}, Message: ${response.message}, Body: $errorBody")
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

        io.legado.app.utils.LogUtils.d("AiSummary", "Cache Path URI String: $cachePathUriString")
        io.legado.app.utils.LogUtils.d("AiSummary", "Save Format: $saveFormat")
        io.legado.app.utils.LogUtils.d("AiSummary", "Save Mode: $saveMode")

        val fileName = "${book.name}"
        val fileExtension = if (saveFormat == "json") "json" else "txt"

        try {
            val cacheUri = Uri.parse(cachePathUriString)
            val pickedDir = try {
                DocumentFile.fromTreeUri(requireContext(), cacheUri)
            } catch (e: IllegalArgumentException) {
                io.legado.app.utils.LogUtils.e("AiSummary", "Malformed cache URI: $cachePathUriString, Error: ${e.message}")
                toastOnUi("AI摘要保存失败：缓存目录URI格式错误")
                return
            } catch (e: SecurityException) {
                io.legado.app.utils.LogUtils.e("AiSummary", "Permission denied for cache URI: $cachePathUriString, Error: ${e.message}")
                toastOnUi("AI摘要保存失败：无权限访问缓存目录，请重新选择")
                return
            }
            io.legado.app.utils.LogUtils.d("AiSummary", "Picked directory: $pickedDir")
            if (pickedDir != null) {
                io.legado.app.utils.LogUtils.d("AiSummary", "Picked directory exists: ${pickedDir.exists()}")
                io.legado.app.utils.LogUtils.d("AiSummary", "Picked directory is directory: ${pickedDir.isDirectory()}")
            }

            if (pickedDir == null || !pickedDir.exists() || !pickedDir.isDirectory) {
                io.legado.app.utils.LogUtils.e("AiSummary", "Invalid or non-existent cache directory: $cachePathUriString")
                toastOnUi("AI摘要保存失败：缓存目录无效或不存在")
                return
            }

            // Create book-specific subdirectory
            var bookDir = pickedDir.findFile(book.name)
            if (bookDir == null || !bookDir.exists() || !bookDir.isDirectory) {
                bookDir = pickedDir.createDirectory(book.name)
                if (bookDir == null) {
                    io.legado.app.utils.LogUtils.e("AiSummary", "Failed to create book directory: ${book.name}")
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
                        io.legado.app.utils.LogUtils.e("AiSummary", "Failed to create or find file: $fileName.$fileExtension")
                        toastOnUi("AI摘要保存失败：无法创建或找到文件")
                        return
                    }

                    if (saveMode == "overwrite") {
                        requireContext().contentResolver.openOutputStream(targetFile.uri)?.use { outputStream: OutputStream ->
                            outputStream.write(summary.toByteArray())
                        }
                        io.legado.app.utils.LogUtils.d("AiSummary", "Summary saved (overwrite): ${targetFile.uri}")
                        toastOnUi("AI摘要保存成功")
                    } else { // append
                        requireContext().contentResolver.openOutputStream(targetFile.uri, "wa")?.use { outputStream: OutputStream ->
                            outputStream.write("""

--- ${chapter.title} ---

""".toByteArray(Charsets.UTF_8))
                            outputStream.write(summary.toByteArray())
                        }
                        io.legado.app.utils.LogUtils.d("AiSummary", "Summary saved (append): ${targetFile.uri}")
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
                        io.legado.app.utils.LogUtils.d("AiSummary", "Summary saved (new_file): ${newFile.uri}")
                        toastOnUi(getString(R.string.summary_save_success, newFile.uri.path))
                    } else {
                        io.legado.app.utils.LogUtils.e("AiSummary", "Failed to create new file: $newFileName")
                        toastOnUi(getString(R.string.summary_save_failed, "无法创建新文件"))
                    }
                }
            }
        } catch (e: Exception) {
            io.legado.app.utils.LogUtils.e("AiSummary", "Error saving summary to file: ${e.message}")
            toastOnUi("AI摘要保存失败：${e.message}")
        }
    }
}
