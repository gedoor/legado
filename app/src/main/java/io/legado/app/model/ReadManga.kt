package io.legado.app.model

import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookProgress
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.ReadRecord
import io.legado.app.help.AppWebDav
import io.legado.app.help.ConcurrentRateLimiter
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.readSimulating
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.book.update
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.globalExecutor
import io.legado.app.model.recyclerView.MangaContent
import io.legado.app.model.recyclerView.ReaderLoading
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.mapIndexed
import io.legado.app.utils.runOnUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.math.min

@Suppress("MemberVisibilityCanBePrivate")
object ReadManga : CoroutineScope by MainScope() {
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
    var chapterTitle: String = ""
    var readStartTime: Long = System.currentTimeMillis()
    private val readRecord = ReadRecord()
    private val loadingChapters = arrayListOf<Int>()
    var simulatedChapterSize = 0
    var mCallback: Callback? = null
    var mFirstLoading = false
    var gameOver = false
    var mTopChapter: BookChapter? = null
    var preDownloadTask: Job? = null
    val downloadedChapters = hashSetOf<Int>()
    val downloadFailChapters = hashMapOf<Int, Int>()
    private val downloadLoadingChapters = arrayListOf<Int>()
    var isMangaMode = false
    val downloadScope = CoroutineScope(SupervisorJob() + IO)
    var rateLimiter = ConcurrentRateLimiter(null)

    fun saveRead(pageChanged: Boolean = false) {
        executor.execute {
            val book = book ?: return@execute
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
        ReadManga.book = book
        isMangaMode = true
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
        ReadManga.book = book
        isMangaMode = true
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
            rateLimiter = ConcurrentRateLimiter(it)
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

    private fun addDownloadLoading(index: Int): Boolean {
        synchronized(this) {
            if (downloadLoadingChapters.contains(index)) return false
            downloadLoadingChapters.add(index)
            return true
        }
    }


    fun removeDownloadLoading(index: Int) {
        synchronized(this) {
            downloadLoadingChapters.remove(index)
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
     * 获取正文
     */
    private suspend fun getContent(
        scope: CoroutineScope,
        chapter: BookChapter,
    ) {
        val book = book ?: return removeLoading(chapter.index)
        val bookSource = bookSource
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
                val book = book!!
                appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                    getContent(
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
        inBookshelf = false
        tocChanged = false
        chapterChanged = false
        book = null
        durChapterPagePos = 0
        isMangaMode = false
        durChapterPageCount = 0
        durChapterPos = 0
        durChapterCount = 0
        bookSource = null
        chapterTitle = ""
        loadingChapters.clear()
        simulatedChapterSize = 0
        mCallback = null
        mFirstLoading = false
        gameOver = false
        mTopChapter = null
        preDownloadTask?.cancel()
        preDownloadTask = null
        downloadedChapters.clear()
        downloadFailChapters.clear()
        downloadLoadingChapters.clear()
        downloadScope.coroutineContext.cancelChildren()
        coroutineContext.cancelChildren()
    }

    /**
     * 内容加载完成
     */
    suspend fun contentLoadFinish(
        chapter: BookChapter,
        content: String,
        book: Book,
    ) {
        if (mTopChapter != null && mTopChapter!!.title != chapterTitle
            && BookHelp.hasContent(book, mTopChapter!!)
        ) {
            BookHelp.delContent(book, chapter)
        }
        mTopChapter = chapter
        chapterTitle = chapter.title
        if (chapter.index !in durChapterPagePos - 1..durChapterPagePos + 1) {
            return
        }
        if (content.isNotEmpty()) {
            val list = flow {
                val matcher = AppPattern.imgPattern.matcher(content)
                while (matcher.find()) {
                    val src = matcher.group(1) ?: continue
                    val mSrc = NetworkUtils.getAbsoluteURL(chapter.url, src)
                    emit(mSrc)
                }
            }.distinctUntilChanged().mapIndexed { index, src ->
                MangaContent(
                    mChapterPageCount = durChapterPageCount,
                    mChapterPagePos = durChapterPagePos.plus(1),
                    mChapterNextPagePos = durChapterPagePos.plus(1),
                    mImageUrl = src,
                    mDurChapterPos = index.plus(1),
                    mChapterName = chapterTitle
                )
            }.toList().apply {
                this.forEach {
                    it.mDurChapterCount = this.size
                }
                durChapterCount = this.size
            }
            val contentList = mutableListOf<Any>()
            contentList.add(
                ReaderLoading(
                    durChapterPagePos,
                    "阅读 ${chapter.title}",
                    mNextChapterIndex = durChapterPagePos.plus(1)
                )
            )
            contentList.addAll(list)
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
            saveRead()
            loadContent(durChapterPagePos)
            AppLog.putDebug("moveToNextChapter-curPageChanged()")
            curPageChanged()
        } else {
            AppLog.putDebug("跳转下一章失败,没有下一章")
        }
    }

    fun curPageChanged() {
        upReadTime()
        preDownload()
    }

    @Synchronized
    fun upToc(index: Int) {
        val bookSource = bookSource ?: return
        val book = book ?: return
        if (!book.canUpdate) return
        if (System.currentTimeMillis() - book.lastCheckTime < 600000) {
            runOnUI {
                mCallback?.noData()
            }
            return
        }
        book.lastCheckTime = System.currentTimeMillis()
        WebBook.getChapterList(this, bookSource, book).onSuccess(IO) { cList ->
            if (book.bookUrl == ReadManga.book?.bookUrl
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
                gameOver = true
                runOnUI {
                    mCallback?.noData()
                }
            }
        }.onError {
            mCallback?.noData()
        }
    }

    private suspend fun getContent(
        bookSource: BookSource,
        scope: CoroutineScope,
        chapter: BookChapter,
        book: Book,
    ) {
        if (BookHelp.hasContent(book, chapter)) {
            BookHelp.getContent(book, chapter)?.apply {
                contentLoadFinish(chapter, this, book)
                runOnUI {
                    mCallback?.loadComplete()
                }
                if (durChapterPagePos >= durChapterPageCount.minus(1)) {
                    gameOver = true
                    runOnUI {
                        mCallback?.noData()
                    }
                }
            } ?: downloadNetworkContent(bookSource, scope, chapter, book, success = {
                contentLoadFinish(chapter, it, book)
                runOnUI {
                    mCallback?.loadComplete()
                }
            }, error = {
                removeLoading(chapter.index)
                runOnUI {
                    mCallback?.loadFail("加载内容失败")
                }
            })
        } else {
            downloadNetworkContent(bookSource, scope, chapter, book, success = {
                contentLoadFinish(chapter, it, book)
                runOnUI {
                    mCallback?.loadComplete()
                }
            }, error = {
                removeLoading(chapter.index)
                runOnUI {
                    mCallback?.loadFail("加载内容失败")
                }
            })
        }
    }

    private fun downloadNetworkContent(
        bookSource: BookSource,
        scope: CoroutineScope,
        chapter: BookChapter,
        book: Book,
        success: suspend (String) -> Unit = {},
        error: suspend () -> Unit = {},
    ) {
        WebBook.getContent(
            scope,
            bookSource,
            book,
            chapter,
            start = CoroutineStart.LAZY,
            executeContext = IO,
            needSave = true
        ).onSuccess { content ->
            success.invoke(content)
        }.onError {
            error.invoke()
        }.start()
    }

    private fun preDownload() {
        if (book?.isLocal == true) return
        executor.execute {
            if (AppConfig.preDownloadNum < 2) {
                return@execute
            }
            preDownloadTask?.cancel()
            preDownloadTask = launch(IO) {
                //预下载
                launch {
                    val maxChapterIndex =
                        min(durChapterPagePos + AppConfig.preDownloadNum, durChapterPageCount)
                    for (i in durChapterPagePos.plus(2)..maxChapterIndex) {
                        if (downloadedChapters.contains(i)) continue
                        if ((downloadFailChapters[i] ?: 0) >= 3) continue
                        downloadIndex(i)
                    }
                }
                launch {
                    val minChapterIndex = durChapterPagePos - min(5, AppConfig.preDownloadNum)
                    for (i in durChapterPagePos.minus(2) downTo minChapterIndex) {
                        if (downloadedChapters.contains(i)) continue
                        if ((downloadFailChapters[i] ?: 0) >= 3) continue
                        downloadIndex(i)
                    }
                }
            }
        }
    }

    private suspend fun downloadIndex(index: Int) {
        if (index < 0) return
        if (index > durChapterPageCount - 1) {
            upToc()
            return
        }
        val book = book ?: return
        if (addDownloadLoading(index)) {
            try {
                appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                    if (BookHelp.hasContent(book, chapter)) {
                        removeDownloadLoading(chapter.index)
                        downloadedChapters.add(chapter.index)
                    } else {
                        delay(1000)
                        downloadNetworkContent(
                            bookSource!!,
                            downloadScope,
                            chapter,
                            book,
                            success = {
                                downloadedChapters.add(chapter.index)
                                downloadFailChapters.remove(chapter.index)
                            },
                            error = {
                                downloadFailChapters[chapter.index] =
                                    (downloadFailChapters[chapter.index] ?: 0) + 1
                                removeDownloadLoading(chapter.index)
                            })
                    }
                } ?: removeDownloadLoading(index)
            } catch (_: Exception) {
                removeLoading(index)
            }
        }
    }

    @Synchronized
    fun upToc() {
        val bookSource = bookSource ?: return
        val book = book ?: return
        if (!book.canUpdate) return
        if (System.currentTimeMillis() - book.lastCheckTime < 600000) return
        book.lastCheckTime = System.currentTimeMillis()
        WebBook.getChapterList(this, bookSource, book).onSuccess(IO) { cList ->
            if (book.bookUrl == ReadManga.book?.bookUrl
                && cList.size > durChapterPageCount
            ) {
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*cList.toTypedArray())
                saveRead()
                durChapterPageCount = cList.size
                simulatedChapterSize = book.simulatedTotalChapterNum()
            }
        }
    }

    fun uploadProgress(successAction: (() -> Unit)? = null) {
        book?.let {
            launch(IO) {
                AppWebDav.uploadBookProgress(it)
                ensureActive()
                it.update()
                successAction?.invoke()
            }
        }
    }


    /**
     * 同步阅读进度
     * 如果当前进度快于服务器进度或者没有进度进行上传，如果慢与服务器进度则执行传入动作
     */
    fun syncProgress(
        newProgressAction: ((progress: BookProgress) -> Unit)? = null,
        uploadSuccessAction: (() -> Unit)? = null,
        syncSuccessAction: (() -> Unit)? = null,
    ) {
        if (!AppConfig.syncBookProgress) return
        book?.let {
            Coroutine.async {
                AppWebDav.getBookProgress(it)
            }.onError {
                AppLog.put("拉取阅读进度失败", it)
            }.onSuccess { progress ->
                if (progress == null || progress.durChapterIndex < it.durChapterIndex ||
                    (progress.durChapterIndex == it.durChapterIndex
                            && progress.durChapterPos < it.durChapterPos)
                ) {
                    // 服务器没有进度或者进度比服务器快，上传现有进度
                    Coroutine.async {
                        AppWebDav.uploadBookProgress(BookProgress(it), uploadSuccessAction)
                        it.update()
                    }
                } else if (progress.durChapterIndex > it.durChapterIndex ||
                    progress.durChapterPos > it.durChapterPos
                ) {
                    // 进度比服务器慢，执行传入动作
                    newProgressAction?.invoke(progress)
                } else {
                    syncSuccessAction?.invoke()
                }
            }
        }
    }

    fun setProgress(progress: BookProgress) {
        if (progress.durChapterIndex < durChapterPageCount &&
            (durChapterPagePos != progress.durChapterIndex
                    || durChapterPos != progress.durChapterPos)
        ) {
            chapterChanged = true
            if (progress.durChapterIndex == durChapterPagePos) {
                durChapterPos = progress.durChapterPos
                mCallback?.adjustmentProgress()
            } else {
                durChapterPagePos = progress.durChapterIndex
                durChapterPos = progress.durChapterPos
                if (addLoading(durChapterPagePos)) {
                    loadContent(durChapterPagePos)
                } else {
                    Coroutine.async {
                        val book = book!!
                        appDb.bookChapterDao.getChapter(book.bookUrl, durChapterPagePos)
                            ?.let { chapter ->
                                getContent(
                                    downloadScope,
                                    chapter,
                                )
                            }
                    }.onError {
                        AppLog.put("加载正文出错\n${it.localizedMessage}")
                    }
                }
            }
            saveRead()
        }
    }

    interface Callback {
        fun loadContentFinish(list: MutableList<Any>)
        fun loadComplete()
        fun loadFail(msg: String)
        fun noData()
        fun adjustmentProgress()
        fun sureNewProgress(progress: BookProgress)
        val chapterList: MutableList<Any>
    }
}