package io.legado.app.ui.book.read

import android.app.Application
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.ZhanweifuDialogContentEditBinding
import io.legado.app.help.book.isLocal
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ReadBook
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.book.read.content.ZhanweifuBookHelp
import io.legado.app.utils.applyTint
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 内容编辑
 */
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
                val chapter = withContext(IO) {
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
        viewModel.zhanweifuInitContent {
            binding.zhanweifuContentView.setText(it)
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
                R.id.zhanweifu_menu_reset -> viewModel.zhanweifuInitContent(true) { content ->
                    binding.zhanweifuContentView.setText(content)
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
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
                    withContext(IO) {
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
        var content: String? = null

        fun zhanweifuInitContent(reset: Boolean = false, success: (String) -> Unit) {
            execute {
                val book = ReadBook.book ?: return@execute null
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute null
                if (reset) {
                    content = null
                    ZhanweifuBookHelp.zhanweifuDelContent(book, chapter)
                    if (!book.isLocal) ReadBook.bookSource?.let { bookSource ->
                        WebBook.getContentAwait(bookSource, book, chapter)
                    }
                }
                return@execute content ?: let {
                    val content = ZhanweifuBookHelp.zhanweifuGetContent(book, chapter) ?: return@let null
                    content
                }
            }.onStart {
                loadStateLiveData.postValue(true)
            }.onSuccess {
                content = it
                success.invoke(it ?: "")
            }.onFinally {
                loadStateLiveData.postValue(false)
            }
        }

    }

}