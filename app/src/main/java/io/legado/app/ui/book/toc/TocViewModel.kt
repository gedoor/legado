package io.legado.app.ui.book.toc


import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeToOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TocViewModel(application: Application) : BaseViewModel(application) {
    var bookUrl: String = ""
    var bookData = MutableLiveData<Book>()
    var chapterListCallBack: ChapterListCallBack? = null
    var bookMarkCallBack: BookmarkCallBack? = null
    var searchKey: String? = null

    fun initBook(bookUrl: String) {
        this.bookUrl = bookUrl
        execute {
            appDb.bookDao.getBook(bookUrl)?.let {
                bookData.postValue(it)
            }
        }
    }

    fun upBookTocRule(book: Book, finally: () -> Unit) {
        execute {
            appDb.bookDao.update(book)
            LocalBook.getChapterList(book).let {
                book.latestChapterTime = System.currentTimeMillis()
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*it.toTypedArray())
                appDb.bookDao.update(book)
                bookData.postValue(book)
            }
        }.onFinally {
            finally.invoke()
        }
    }

    fun reverseToc(success: (book: Book) -> Unit) {
        execute {
            bookData.value?.apply {
                setReverseToc(!getReverseToc())
                val toc = appDb.bookChapterDao.getChapterList(bookUrl)
                val newToc = toc.reversed()
                newToc.forEachIndexed { index, bookChapter ->
                    bookChapter.index = index
                }
                appDb.bookChapterDao.insert(*newToc.toTypedArray())
            }
        }.onSuccess {
            it?.let(success)
        }
    }

    fun startChapterListSearch(newText: String?) {
        chapterListCallBack?.upChapterList(newText)
    }

    fun startBookmarkSearch(newText: String?) {
        bookMarkCallBack?.upBookmark(newText)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun saveBookmark(treeUri: Uri) {
        execute {
            val book = bookData.value
                ?: throw NoStackTraceException(context.getString(R.string.no_book))
            val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
            val bookmark = appDb.bookmarkDao.getByBook(book.name, book.author)
            if (treeUri.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                    ?.createFile("", "bookmark-${dateFormat.format(Date())}")
                doc?.let {
                    context.contentResolver.openOutputStream(doc.uri)!!.use {
                        GSON.writeToOutputStream(it, bookmark)
                    }
                }
            } else {
                val path = treeUri.path!!
                val file = FileUtils.createFileIfNotExist(
                    File(path), "bookmark-${
                        dateFormat.format(
                            Date()
                        )
                    }"
                )
                FileOutputStream(file).use {
                    GSON.writeToOutputStream(it, bookmark)
                }
            }
        }.onError {
            AppLog.put("导出失败\n${it.localizedMessage}", it, true)
        }.onSuccess {
            context.toastOnUi("导出成功")
        }
    }

    interface ChapterListCallBack {
        fun upChapterList(searchKey: String?)

        fun clearDisplayTitle()
    }

    interface BookmarkCallBack {
        fun upBookmark(searchKey: String?)
    }
}