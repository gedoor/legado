package io.legado.app.ui.book.import

import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.databinding.ActivityImportBookBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseImportBookActivity<VM : ViewModel> : VMBaseActivity<ActivityImportBookBinding, VM>() {

    final override val binding by viewBinding(ActivityImportBookBinding::inflate)

    private var localBookTreeSelectListener: ((Boolean) -> Unit)? = null
    protected val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }

    val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
            localBookTreeSelectListener?.invoke(true)
        } ?: localBookTreeSelectListener?.invoke(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSearchView()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let {
                if (it is EditText) {
                    it.clearFocus()
                    it.hideSoftInput()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 设置书籍保存位置
     */
    protected suspend fun setBookStorage() = suspendCoroutine { block ->
        localBookTreeSelectListener = {
            localBookTreeSelectListener = null
            block.resume(it)
        }
        //测试书籍保存位置是否设置
        if (!AppConfig.defaultBookTreeUri.isNullOrBlank()) {
            localBookTreeSelectListener = null
            block.resume(true)
            return@suspendCoroutine
        }
        //测试读写??
        val storageHelp = String(assets.open("storageHelp.md").readBytes())
        val hint = getString(R.string.select_book_folder)
        alert(hint, storageHelp) {
            yesButton {
                localBookTreeSelect.launch {
                    title = hint
                }
            }
            noButton {
                localBookTreeSelectListener = null
                block.resume(false)
            }
            onCancelled {
                localBookTreeSelectListener = null
                block.resume(false)
            }
        }
    }

    abstract fun onSearchTextChange(newText: String?)

    protected fun startReadBook(bookUrl: String) {
        startActivity<ReadBookActivity> {
            putExtra("bookUrl", bookUrl)
        }
    }

    protected fun onArchiveFileClick(fileDoc: FileDoc) {
        val fileNames = ArchiveUtils.getArchiveFilesName(fileDoc) {
            it.matches(AppPattern.bookFileRegex)
        }
        if (fileNames.size == 1) {
            val name = fileNames[0]
            appDb.bookDao.getBookByFileName(name)?.let {
                startReadBook(it.bookUrl)
            } ?: showImportAlert(fileDoc, name)
        } else {
            showSelectBookReadAlert(fileDoc, fileNames)
        }
    }

    private fun showSelectBookReadAlert(fileDoc: FileDoc, fileNames: List<String>) {
        if (fileNames.isEmpty()) {
            toastOnUi(R.string.unsupport_archivefile_entry)
            return
        }
        selector(
            R.string.start_read,
            fileNames
        ) { _, name, _ ->
            appDb.bookDao.getBookByFileName(name)?.let {
                startReadBook(it.bookUrl)
            } ?: showImportAlert(fileDoc, name)
        }
    }

    /* 添加压缩包内指定文件到书架 */
    private inline fun addArchiveToBookShelf(
        fileDoc: FileDoc,
        fileName: String,
        onSuccess: (String) -> Unit
    ) {
        LocalBook.importArchiveFile(fileDoc.uri, fileName) {
            it.contains(fileName)
        }.firstOrNull()?.run {
            onSuccess.invoke(bookUrl)
        }
    }

    /* 提示是否重新导入所点击的压缩文件 */
    private fun showImportAlert(fileDoc: FileDoc, fileName: String) {
        alert(
            R.string.draw,
            R.string.no_book_found_bookshelf
        ) {
            okButton {
                addArchiveToBookShelf(fileDoc, fileName) {
                    startReadBook(it)
                }
            }
            noButton()
        }
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onSearchTextChange(newText)
                return false
            }
        })
    }

}