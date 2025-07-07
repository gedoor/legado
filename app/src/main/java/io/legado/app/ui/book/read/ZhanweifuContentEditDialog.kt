package io.legado.app.ui.book.read

import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.stream.JsonReader
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.ZhanweifuDialogContentEditBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.content.ZhanweifuBookHelp
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit


class ZhanweifuContentEditDialog : BaseDialogFragment(R.layout.zhanweifu_dialog_content_edit) {

    val binding by viewBinding(ZhanweifuDialogContentEditBinding::bind)
    val viewModel by viewModels<ZhanweifuContentEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.zhanweifuToolBar.setBackgroundColor(primaryColor)
        binding.zhanweifuToolBar.title = ReadBook.curTextChapter?.title
        zhanweifuInitMenu()
        binding.zhanweifuToolBar.setOnClickListener {
            lifecycleScope.launch {
                val book = ReadBook.book ?: return@launch
                val chapter = withContext(Dispatchers.IO) {
                    appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
                } ?: return@launch
                zhanweifuEditTitle(chapter)
            }
        }
        viewModel.loadStateLiveData.observe(viewLifecycleOwner) {
            if (it) {
                binding.zhanweifuRlLoading.visible()
            } else {
                binding.zhanweifuRlLoading.gone()
            }
        }
        viewModel.contentStream.observe(viewLifecycleOwner) {
            binding.zhanweifuContentView.append(it)
        }
        viewModel.zhanweifuInitContent { initialContent ->
            if (binding.zhanweifuContentView.text.isNullOrEmpty()) {
                binding.zhanweifuContentView.setText(initialContent)
            }
            binding.zhanweifuContentView.post {
                binding.zhanweifuContentView.apply {
                    val lineIndex = layout.getLineForOffset(ReadBook.durChapterPos)
                    val lineHeight = layout.getLineTop(lineIndex)
                    scrollTo(0, lineHeight)
                }
            }
        }
    }

    private fun zhanweifuInitMenu() {
        binding.zhanweifuToolBar.inflateMenu(R.menu.zhanweifu_content_edit)
        binding.zhanweifuToolBar.menu.applyTint(requireContext())
        binding.zhanweifuToolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.zhanweifu_menu_save -> {
                    zhanweifuSave()
                    dismiss()
                }
                R.id.zhanweifu_menu_reset -> {
                    binding.zhanweifuContentView.setText("")
                    viewModel.zhanweifuInitContent(true) { content ->
                        binding.zhanweifuContentView.setText(content)
                        ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                    }
                }
                R.id.zhanweifu_menu_copy_all -> requireContext()
                    .sendToClip("${binding.zhanweifuToolBar.title}\n${binding.zhanweifuContentView.text}")
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun zhanweifuEditTitle(chapter: BookChapter) {
        alert {
            setTitle(R.string.edit)
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.editView.setText(chapter.title)
            setCustomView(alertBinding.root)
            okButton {
                chapter.title = alertBinding.editView.text.toString()
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        appDb.bookChapterDao.update(chapter)
                    }
                    binding.zhanweifuToolBar.title = chapter.getDisplayTitle()
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        zhanweifuSave()
    }

    private fun zhanweifuSave() {
        val content = binding.zhanweifuContentView.text?.toString() ?: return
        Coroutine.async {
            val book = ReadBook.book ?: return@async
            val chapter = appDb.bookChapterDao
                .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                ?: return@async
            ZhanweifuBookHelp.zhanweifuSaveText(book, chapter, content)
            ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
        }
    }

    class ZhanweifuContentEditViewModel(application: Application) : BaseViewModel(application) {
        val loadStateLiveData = MutableLiveData<Boolean>()
        val contentStream = MutableLiveData<String>()

        fun zhanweifuInitContent(reset: Boolean = false, success: (String) -> Unit) {
            execute {
                val book = ReadBook.book ?: return@execute
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute

                if (reset) {
                    ZhanweifuBookHelp.zhanweifuDelContent(book, chapter)
                }

                // 1. 检查对话框自身缓存
                val dialogContent = ZhanweifuBookHelp.zhanweifuGetContent(book, chapter)
                if (!dialogContent.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke(dialogContent) }
                    return@execute
                }

                // 2. 检查AI摘要缓存
                val cachedSummary = ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter)
                if (!cachedSummary.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke(cachedSummary) }
                    return@execute
                }

                // 3. 流式API请求
                val chapterContent = ReadBook.curTextChapter?.getContent() ?: ""
                if (chapterContent.isEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke("本章无内容") }
                    return@execute
                }

                val apiKey = AppConfig.aiSummaryApiKey
                val apiUrl = AppConfig.aiSummaryApiUrl
                if (apiKey.isNullOrEmpty() || apiUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke("请先设置AI摘要的API Key和URL") }
                    return@execute
                }

                streamRequest(apiUrl, apiKey, chapterContent)

            }.onStart {
                loadStateLiveData.postValue(true)
            }.onFinally {
                loadStateLiveData.postValue(false)
            }
        }

        private suspend fun streamRequest(apiUrl: String, apiKey: String, content: String) {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val messages = mutableListOf<Map<String, String>>()
            messages.add(mapOf("role" to "system", "content" to (AppConfig.aiSummarySystemPrompt ?: "请总结以下内容：")))
            messages.add(mapOf("role" to "user", "content" to content))
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
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    handleStreamResponse(response)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    contentStream.postValue("\n\n[请求失败: ${e.message}]")
                }
            }
        }

        private suspend fun handleStreamResponse(response: Response) {
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
                                    contentStream.postValue(text)
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore parsing errors for now
                        }
                    }
                }
            } catch (e: IOException) {
                 withContext(Dispatchers.Main) {
                    contentStream.postValue("\n\n[读取数据流失败: ${e.message}]")
                }
            }
        }

    }

}
