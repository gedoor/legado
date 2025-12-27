package io.legado.app.ui.book.import

import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.databinding.ActivityImportBookBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.ArchiveUtils
import io.legado.app.utils.FileDoc
import io.legado.app.utils.applyTint
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

abstract class BaseImportBookActivity<VM : ViewModel> :
    VMBaseActivity<ActivityImportBookBinding, VM>() {

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

    /**
     * 设置书籍保存位置
     */
    protected suspend fun setBookStorage() = suspendCancellableCoroutine sc@{ block ->
        localBookTreeSelectListener = {
            localBookTreeSelectListener = null
            block.resume(it)
        }
        //测试书籍保存位置是否设置
        if (!AppConfig.defaultBookTreeUri.isNullOrBlank()) {
            localBookTreeSelectListener = null
            block.resume(true)
            return@sc
        }
        //测试读写??
        val storageHelp = String(assets.open("storageHelp.md").readBytes())
        val hint = getString(R.string.select_book_folder)
        alert(hint, storageHelp) {
            okButton {
                localBookTreeSelect.launch {
                    title = hint
                }
            }
            cancelButton {
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

    protected fun startReadBook(book: Book) {
        startActivityForBook(book)
    }

    protected fun onArchiveFileClick(fileDoc: FileDoc) {
        val fileNames = ArchiveUtils.getArchiveFilesName(fileDoc) {
            it.matches(AppPattern.bookFileRegex)
        }
        if (fileNames.size == 1) {
            val name = fileNames[0]
            appDb.bookDao.getBookByFileName(name)?.let {
                startReadBook(it)
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
                startReadBook(it)
            } ?: showImportAlert(fileDoc, name)
        }
    }

    /* 添加压缩包内指定文件到书架 */
    private inline fun addArchiveToBookShelf(
        fileDoc: FileDoc,
        fileName: String,
        onSuccess: (Book) -> Unit
    ) {
        LocalBook.importArchiveFile(fileDoc.uri, fileName) {
            it.contains(fileName)
        }.firstOrNull()?.run {
            onSuccess.invoke(this)
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
        searchView.isSubmitButtonEnabled = true
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