package io.legado.app.model

import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReadRecord
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.readSimulating
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.globalExecutor
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import org.jsoup.Jsoup

@Suppress("MemberVisibilityCanBePrivate")
object ReadMange : CoroutineScope by MainScope() {
    var inBookshelf = false
    var tocChanged = false
    var chapterChanged = false
    var book: Book? = null
    val executor = globalExecutor
    var durChapterIndex = 0
    var durChapterPos = 0
    var chapterSize = 0
    var bookSource: BookSource? = null
    var readStartTime: Long = System.currentTimeMillis()
    private val readRecord = ReadRecord()
    private val loadingChapters = arrayListOf<Int>()
    var simulatedChapterSize = 0
    var mCallback: Callback? = null
    val downloadScope = CoroutineScope(SupervisorJob() + IO)

    fun saveRead(pageChanged: Boolean = false) {
        executor.execute {
            val book = ReadMange.book ?: return@execute
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            val chapterChanged = book.durChapterIndex != durChapterIndex
            book.durChapterIndex = durChapterIndex
            book.durChapterPos = durChapterPos
            if (!pageChanged || chapterChanged) {
                appDb.bookChapterDao.getChapter(book.bookUrl, durChapterIndex)?.let {
                    book.durChapterTitle = it.getDisplayTitle(
                        ContentProcessor.get(book.name, book.origin).getTitleReplaceRules(),
                        book.getUseReplaceRule()
                    )
                }
            }
            appDb.bookDao.update(book)
        }
    }

    fun upData(book: Book) {
        ReadMange.book = book
        chapterSize = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        simulatedChapterSize = if (book.readSimulating()) {
            book.simulatedTotalChapterNum()
        } else {
            chapterSize
        }

        if (durChapterIndex != book.durChapterIndex || tocChanged) {
            durChapterIndex = book.durChapterIndex
            durChapterPos = book.durChapterPos
        }
        upWebBook(book)

        synchronized(this) {
            loadingChapters.clear()
        }
    }

    fun resetData(book: Book) {
        ReadMange.book = book
        readRecord.bookName = book.name
        readRecord.readTime = appDb.readRecordDao.getReadTime(book.name) ?: 0
        chapterSize = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        simulatedChapterSize = if (book.readSimulating()) {
            book.simulatedTotalChapterNum()
        } else {
            chapterSize
        }
        durChapterIndex = book.durChapterIndex
        durChapterPos = book.durChapterPos
        upWebBook(book)
        synchronized(this) {
            loadingChapters.clear()
        }
    }

    fun upWebBook(book: Book) {
        appDb.bookSourceDao.getBookSource(book.origin)?.let {
            bookSource = it
        } ?: let {
            bookSource = null
        }
    }


    //每次切换章节更新阅读记录
    fun upReadTime() {
        executor.execute {
            if (!AppConfig.enableReadRecord) {
                return@execute
            }
            readRecord.readTime = readRecord.readTime + System.currentTimeMillis() - readStartTime
            readStartTime = System.currentTimeMillis()
            readRecord.lastRead = System.currentTimeMillis()
            appDb.readRecordDao.insert(readRecord)
        }
    }

    private fun addLoading(index: Int): Boolean {
        synchronized(this) {
            if (loadingChapters.contains(index)) return false
            loadingChapters.add(index)
            return true
        }
    }

    fun removeLoading(index: Int) {
        synchronized(this) {
            loadingChapters.remove(index)
        }
    }


    /**
     * 下载正文
     */
    private fun download(
        scope: CoroutineScope,
        chapter: BookChapter,
    ) {
        val book = ReadMange.book ?: return removeLoading(chapter.index)
        val bookSource = ReadMange.bookSource
        if (bookSource != null) {
            CacheBook.getOrCreate(bookSource, book).download(scope, chapter)
        } else {
            val msg = if (book.isLocal) "无内容" else "没有书源"
            contentLoadFinish(
                book,
                chapter,
                "加载正文失败\n$msg",
            )
        }
    }


    fun loadContent() {
        loadContent(durChapterIndex)
    }

    fun loadContent(
        index: Int,
    ) {
        if (addLoading(index)) {
            Coroutine.async {
                val book = ReadMange.book!!
                appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                    BookHelp.getContent(book, chapter)?.let {
                        contentLoadFinish(
                            book,
                            chapter,
                            it,
                        )
                    } ?: download(
                        downloadScope,
                        chapter,
                    )
                } ?: removeLoading(index)
            }.onError {
                removeLoading(index)
                AppLog.put("加载正文出错\n${it.localizedMessage}")
            }
        }
    }


    /**
     * 注册回调
     */
    fun register(cb: Callback) {
        mCallback = cb
    }

    /**
     * 取消注册回调
     */
    fun unregister(cb: Callback) {
        if (mCallback === cb) {
            mCallback = null
        }
        downloadScope.coroutineContext.cancelChildren()
        coroutineContext.cancelChildren()
        ImageProvider.clear()
    }

    /**
     * 内容加载完成
     */
    fun contentLoadFinish(
        book: Book,
        chapter: BookChapter,
        content: String,
    ) {
        if (content.isNotEmpty()) {
            val contentList = mutableListOf<Any>()
            Jsoup.parse(content).select("img").forEach {
                contentList.add(MangeContent(durChapterIndex, it.attr("src")))
            }
            contentList.add(ReaderLoading(durChapterIndex, "下一章,${chapter.title}"))
            mCallback?.loadContentFinish(contentList)
        }

    }

    interface Callback {
        fun loadContentFinish(list: MutableList<Any>)
    }
}