package io.legado.app.model

import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReadRecord
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.readSimulating
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.globalExecutor
import io.legado.app.model.recyclerView.MangeContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.mapIndexed
import io.legado.app.utils.runOnUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

@Suppress("MemberVisibilityCanBePrivate")
object ReadMange : CoroutineScope by MainScope() {
    var inBookshelf = false
    var tocChanged = false
    var chapterChanged = false
    var book: Book? = null
    val executor = globalExecutor
    var durChapterPagePos = 0 //章节位置
    var durChapterPageCount = 0//总章节
    var durChapterCount = 0
    var durChapterPos = 0
    var bookSource: BookSource? = null
    var readStartTime: Long = System.currentTimeMillis()
    private val readRecord = ReadRecord()
    private val loadingChapters = arrayListOf<Int>()
    var simulatedChapterSize = 0
    var mCallback: Callback? = null
    var mFirstLoading = false
    val downloadScope = CoroutineScope(SupervisorJob() + IO)

    fun saveRead(pageChanged: Boolean = false) {
        executor.execute {
            val book = ReadMange.book ?: return@execute
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            val chapterChanged = book.durChapterIndex != durChapterPagePos
            book.durChapterIndex = durChapterPagePos
            book.durChapterPos = durChapterPos
            if (!pageChanged || chapterChanged) {
                appDb.bookChapterDao.getChapter(book.bookUrl, durChapterPagePos)?.let {
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
        durChapterPageCount = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        simulatedChapterSize = if (book.readSimulating()) {
            book.simulatedTotalChapterNum()
        } else {
            durChapterPageCount
        }

        if (durChapterPagePos != book.durChapterIndex || tocChanged) {
            durChapterPagePos = book.durChapterIndex
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
        durChapterPageCount = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        simulatedChapterSize = if (book.readSimulating()) {
            book.simulatedTotalChapterNum()
        } else {
            durChapterPageCount
        }
        durChapterPagePos = book.durChapterIndex
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

    fun loading(index: Int): Boolean {
        synchronized(this) {
            return loadingChapters.contains(index)
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
            getContent(bookSource, scope, chapter, book)
        }
    }


    fun loadContent() {
        loadContent(durChapterPagePos)
    }

    fun loadContent(
        index: Int,
    ) {
        if (addLoading(index)) {
            Coroutine.async {
                val book = ReadMange.book!!
                appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                    download(
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
    fun unregister() {
        mCallback = null
        mFirstLoading = false
        downloadScope.coroutineContext.cancelChildren()
        coroutineContext.cancelChildren()
    }

    /**
     * 内容加载完成
     */
    suspend fun contentLoadFinish(
        chapter: BookChapter,
        content: String,
    ) {
        if (chapter.index !in durChapterPagePos - 1..durChapterPagePos + 1) {
            return
        }
        if (content.isNotEmpty()) {
            val list = flow<Element> {
                Jsoup.parse(content).select("img").forEach {
                    emit(it)
                }
            }.map { element ->
                element.attr("src")
            }.distinctUntilChangedBy {
                it
            }.mapIndexed { index, src ->
                MangeContent(
                    mChapterPageCount = durChapterPageCount,
                    mChapterPagePos = durChapterPagePos,
                    mChapterNextPagePos = durChapterPagePos.plus(1),
                    mImageUrl = src,
                    mDurChapterPos = index.plus(1)
                )
            }.toList().apply {
                this.forEach {
                    it.mDurChapterCount = this.size
                }
            }
            val contentList = mutableListOf<Any>()
            contentList.addAll(list)
            durChapterCount = contentList.size
            contentList.add(
                ReaderLoading(
                    durChapterPagePos,
                    "已读完 ${chapter.title}",
                    mNextChapterIndex = durChapterPagePos.plus(1)
                )
            )
            runOnUI {
                mCallback?.loadContentFinish(contentList)
            }
        }

    }

    /**
     * 加载下一章
     */
    fun moveToNextChapter(index: Int) {

        if (loading(index)) {
            return
        }

        if (index > durChapterPageCount - 1) {
            upToc(index)
            return
        }
        if (durChapterPagePos < simulatedChapterSize - 1) {
            durChapterPos = 0
            durChapterPagePos = index
            loadContent(durChapterPagePos)
            saveRead()
            AppLog.putDebug("moveToNextChapter-curPageChanged()")
            curPageChanged()
        } else {
            AppLog.putDebug("跳转下一章失败,没有下一章")
        }
    }

    fun curPageChanged() {
        upReadTime()
    }

    @Synchronized
    fun upToc(index: Int) {
        val bookSource = bookSource ?: return
        val book = book ?: return
        if (!book.canUpdate) return
        if (System.currentTimeMillis() - book.lastCheckTime < 600000) return
        book.lastCheckTime = System.currentTimeMillis()
        WebBook.getChapterList(this, bookSource, book).onSuccess(IO) { cList ->
            if (book.bookUrl == ReadMange.book?.bookUrl
                && cList.size > durChapterPageCount
            ) {
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*cList.toTypedArray())
                saveRead()
                durChapterPos = 0
                durChapterPagePos = index
                durChapterPageCount = cList.size
                simulatedChapterSize = book.simulatedTotalChapterNum()
                loadContent(durChapterPagePos)
            } else {
                durChapterPagePos = durChapterPagePos.minus(1)
                saveRead()
                runOnUI {
                    mCallback?.noData()
                }
            }
        }
    }

    private fun getContent(
        bookSource: BookSource,
        scope: CoroutineScope,
        chapter: BookChapter,
        book: Book,
    ) {
        WebBook.getContent(
            scope,
            bookSource,
            book,
            chapter,
            start = CoroutineStart.LAZY,
            executeContext = IO
        ).onSuccess { content ->
            contentLoadFinish(chapter, content)
            runOnUI {
                mCallback?.loadComplete()
            }
        }.onError {
            removeLoading(chapter.index)
            runOnUI {
                mCallback?.loadFail()
            }
        }.start()
    }

    interface Callback {
        fun loadContentFinish(list: MutableList<Any>)
        fun loadComplete()
        fun loadFail()
        fun noData()
        val chapterList: MutableList<Any>
    }
}