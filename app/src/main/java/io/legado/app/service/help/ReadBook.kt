package io.legado.app.service.help

import androidx.lifecycle.MutableLiveData
import io.legado.app.App
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.WebBook
import io.legado.app.ui.book.read.ReadBookViewModel
import io.legado.app.ui.widget.page.TextChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


object ReadBook {

    var titleDate = MutableLiveData<String>()
    var book: Book? = null
    var inBookshelf = false
    var chapterSize = 0
    var callBack: ReadBookViewModel.CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    var webBook: WebBook? = null
    private val loadingChapters = arrayListOf<Int>()


    fun moveToNextChapter(upContent: Boolean): Boolean {
        if (durChapterIndex < chapterSize - 1) {
            durChapterIndex++
            prevTextChapter = curTextChapter
            curTextChapter = nextTextChapter
            nextTextChapter = null
            book?.let {
                if (curTextChapter == null) {
                    loadContent(durChapterIndex)
                } else if (upContent) {
                    callBack?.upContent()
                }
                loadContent(durChapterIndex.plus(1))
                GlobalScope.launch(Dispatchers.IO) {
                    for (i in 2..10) {
                        delay(100)
                        download(durChapterIndex + i)
                    }
                }
            }
            saveRead()
            callBack?.curChapterChanged()
            return true
        } else {
            return false
        }
    }

    fun loadContent(index: Int) {
        book?.let { book ->
            if (addLoading(index)) {
                Coroutine.async {
                    App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                        BookHelp.getContent(book, chapter)?.let {
                            contentLoadFinish(chapter, it)
                            removeLoading(chapter.index)
                        } ?: download(chapter)
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    fun download(index: Int) {
        book?.let { book ->
            if (addLoading(index)) {
                Coroutine.async {
                    App.db.bookChapterDao().getChapter(book.bookUrl, index)?.let { chapter ->
                        if (BookHelp.hasContent(book, chapter)) {
                            removeLoading(chapter.index)
                        } else {
                            download(chapter)
                        }
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    fun download(chapter: BookChapter) {
        book?.let { book ->
            webBook?.getContent(book, chapter)
                ?.onSuccess(Dispatchers.IO) { content ->
                    if (content.isNullOrEmpty()) {
                        contentLoadFinish(chapter, App.INSTANCE.getString(R.string.content_empty))
                        removeLoading(chapter.index)
                    } else {
                        BookHelp.saveContent(book, chapter, content)
                        contentLoadFinish(chapter, content)
                        removeLoading(chapter.index)
                    }
                }?.onError {
                    contentLoadFinish(chapter, it.localizedMessage ?: "未知错误")
                    removeLoading(chapter.index)
                }
        }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (loadingChapters.contains(index)) return false
            loadingChapters.add(index)
            return true
        }
    }

    private fun removeLoading(index: Int) {
        synchronized(this) {
            loadingChapters.remove(index)
        }
    }

    private fun contentLoadFinish(chapter: BookChapter, content: String) {
        Coroutine.async {
            if (chapter.index in durChapterIndex - 1..durChapterIndex + 1) {
                val c = BookHelp.disposeContent(
                    chapter.title,
                    book!!.name,
                    webBook?.bookSource?.bookSourceUrl,
                    content,
                    book!!.useReplaceRule
                )
                callBack?.contentLoadFinish(chapter, c)
            }
        }
    }

    fun saveRead() {
        Coroutine.async {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durPageIndex
                curTextChapter?.let {
                    book.durChapterTitle = it.title
                }
                App.db.bookDao().update(book)
            }
        }
    }

}