package io.legado.app.ui.book.read

import android.app.Application
import android.util.Log
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
import androidx.appcompat.widget.TooltipCompat


class ZhanweifuContentEditDialog : BaseDialogFragment(R.layout.zhanweifu_dialog_content_edit) {

    val binding by viewBinding(ZhanweifuDialogContentEditBinding::bind)
    val viewModel by viewModels<ZhanweifuContentEditViewModel>()
    private var listener: ContentReplaceListener? = null

    interface ContentReplaceListener {
        fun onContentReplace(content: String)
    }

    fun setContentReplaceListener(listener: ContentReplaceListener) {
        this.listener = listener
    }

    override fun onStart() {
        super.onStart() 
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("AiSummary", "ZhanweifuContentEditDialog::onFragmentCreated (片段创建)")
        binding.zhanweifuToolBar.setBackgroundColor(primaryColor)
        binding.zhanweifuToolBar.title = ReadBook.curTextChapter?.title
        zhanweifuInitMenu()
        binding.fabReplaceToggle.setOnClickListener {
            val content = binding.zhanweifuContentView.text?.toString()
            if (!content.isNullOrEmpty()) {
                listener?.onContentReplace(content)
            }
            dismiss()
        }
        TooltipCompat.setTooltipText(binding.fabReplaceToggle, getString(R.string.ai_summary_replace_tooltip))
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
            Log.d("AiSummary", "ViewModel::zhanweifuInitContent (内容初始化开始)")
            execute {
                val book = ReadBook.book ?: return@execute
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute

                if (reset) {
                    Log.d("AiSummary", "ViewModel::zhanweifuInitContent - (重置内容)")
                    ZhanweifuBookHelp.zhanweifuDelContent(book, chapter)
                    ZhanweifuBookHelp.delAiSummaryCache(book, chapter)
                }

                val dialogContent = ZhanweifuBookHelp.zhanweifuGetContent(book, chapter)
                if (!dialogContent.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke(dialogContent) }
                    return@execute
                }

                val cachedSummary = ZhanweifuBookHelp.getAiSummaryFromCache(book, chapter)
                if (!cachedSummary.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke(cachedSummary) }
                    return@execute
                }

                val chapterContent = ReadBook.curTextChapter?.getContent() ?: ""
                if (chapterContent.isEmpty()) {
                    withContext(Dispatchers.Main) { success.invoke("本章无内容") }
                    return@execute
                }
                
                loadStateLiveData.postValue(true)
                val summaryBuilder = StringBuilder()
                ZhanweifuBookHelp.getAiSummary(
                    content = chapterContent,
                    onResponse = { 
                        contentStream.postValue(it)
                        summaryBuilder.append(it)
                    },
                    onFinish = { 
                        loadStateLiveData.postValue(false)
                        val finalSummary = summaryBuilder.toString()
                        if (finalSummary.isNotEmpty()) {
                            ZhanweifuBookHelp.saveAiSummaryToCache(book, chapter, finalSummary)
                        }
                    },
                    onError = { 
                        loadStateLiveData.postValue(false)
                        contentStream.postValue("\n\n[请求失败: $it]") 
                    }
                )

            }
        }

    }

}