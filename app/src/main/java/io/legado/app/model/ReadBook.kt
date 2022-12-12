package io.legado.app.model

import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.*
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.stackTraceStr
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import splitties.init.appCtx
import kotlin.math.min


@Suppress("MemberVisibilityCanBePrivate")
object ReadBook : CoroutineScope by MainScope() {
    var book: Book? = null
    var callBack: CallBack? = null
    var inBookshelf = false
    var tocChanged = false
    var chapterSize = 0
    var durChapterIndex = 0
    var durChapterPos = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    var bookSource: BookSource? = null
    var msg: String? = null
    private val loadingChapters = arrayListOf<Int>()
    private val readRecord = ReadRecord()
    var readStartTime: Long = System.currentTimeMillis()

    /* 跳转进度前进度记录 */
    var lastBookPress: BookProgress? = null

    /* web端阅读进度记录 */
    var webBookProgress: BookProgress? = null

    //暂时保存跳转前进度
    fun saveCurrentBookProcess() {
        if (lastBookPress != null) return //避免进度条连续跳转不能覆盖最初的进度记录
        lastBookPress = book?.let { BookProgress(it) }
    }

    //恢复跳转前进度
    fun restoreLastBookProcess() {
        lastBookPress?.let {
            setProgress(it)
            lastBookPress = null
        }
    }

    fun resetData(book: Book) {
        ReadBook.book = book
        readRecord.bookName = book.name
        readRecord.readTime = appDb.readRecordDao.getReadTime(book.name) ?: 0
        chapterSize = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        durChapterIndex = book.durChapterIndex
        durChapterPos = book.durChapterPos
        isLocalBook = book.isLocal
        clearTextChapter()
        callBack?.upMenuView()
        callBack?.upPageAnim()
        upWebBook(book)
        lastBookPress = null
        webBookProgress = null
        synchronized(this) {
            loadingChapters.clear()
        }
    }

    fun upData(book: Book) {
        ReadBook.book = book
        chapterSize = appDb.bookChapterDao.getChapterCount(book.bookUrl)
        if (durChapterIndex != book.durChapterIndex || tocChanged) {
            durChapterIndex = book.durChapterIndex
            durChapterPos = book.durChapterPos
            clearTextChapter()
        }
        callBack?.upMenuView()
        upWebBook(book)
    }

    fun upWebBook(book: Book) {
        if (book.isLocal) {
            bookSource = null
        } else {
            appDb.bookSourceDao.getBookSource(book.origin)?.let {
                bookSource = it
                if (book.getImageStyle().isNullOrBlank()) {
                    book.setImageStyle(it.getContentRule().imageStyle)
                }
            } ?: let {
                bookSource = null
            }
        }
    }

    fun setProgress(progress: BookProgress) {
        if (progress.durChapterIndex < chapterSize &&
            (durChapterIndex != progress.durChapterIndex
                    || durChapterPos != progress.durChapterPos)
        ) {
            durChapterIndex = progress.durChapterIndex
            durChapterPos = progress.durChapterPos
            clearTextChapter()
            loadContent(resetPageOffset = true)
        }
    }

    fun clearTextChapter() {
        prevTextChapter = null
        curTextChapter = null
        nextTextChapter = null
    }

    fun uploadProgress() {
        book?.let {
            Coroutine.async {
                AppWebDav.uploadBookProgress(it)
            }
        }
    }

    fun upReadTime() {
        Coroutine.async {
            readRecord.readTime = readRecord.readTime + System.currentTimeMillis() - readStartTime
            readStartTime = System.currentTimeMillis()
            readRecord.lastRead = System.currentTimeMillis()
            if (AppConfig.enableReadRecord) {
                appDb.readRecordDao.insert(readRecord)
            }
        }
    }

    fun upMsg(msg: String?) {
        if (ReadBook.msg != msg) {
            ReadBook.msg = msg
            callBack?.upContent()
        }
    }

    fun moveToNextPage(): Boolean {
        var hasNextPage = false
        curTextChapter?.let {
            val nextPagePos = it.getNextPageLength(durChapterPos)
            if (nextPagePos >= 0) {
                hasNextPage = true
                durChapterPos = nextPagePos
                callBack?.upContent()
                saveRead()
            }
        }
        return hasNextPage
    }

    fun moveToNextChapter(upContent: Boolean): Boolean {
        if (durChapterIndex < chapterSize - 1) {
            durChapterPos = 0
            durChapterIndex++
            prevTextChapter = curTextChapter
            curTextChapter = nextTextChapter
            nextTextChapter = null
            if (curTextChapter == null) {
                loadContent(durChapterIndex, upContent, false)
            } else if (upContent) {
                callBack?.upContent()
            }
            loadContent(durChapterIndex.plus(1), upContent, false)
            saveRead()
            callBack?.upMenuView()
            curPageChanged()
            return true
        } else {
            return false
        }
    }

    fun moveToPrevChapter(
        upContent: Boolean,
        toLast: Boolean = true
    ): Boolean {
        if (durChapterIndex > 0) {
            durChapterPos = if (toLast) prevTextChapter?.lastReadLength ?: 0 else 0
            durChapterIndex--
            nextTextChapter = curTextChapter
            curTextChapter = prevTextChapter
            prevTextChapter = null
            if (curTextChapter == null) {
                loadContent(durChapterIndex, upContent, false)
            } else if (upContent) {
                callBack?.upContent()
            }
            loadContent(durChapterIndex.minus(1), upContent, false)
            saveRead()
            callBack?.upMenuView()
            curPageChanged()
            return true
        } else {
            return false
        }
    }

    fun skipToPage(index: Int, success: (() -> Unit)? = null) {
        durChapterPos = curTextChapter?.getReadLength(index) ?: index
        callBack?.upContent {
            success?.invoke()
        }
        curPageChanged()
        saveRead()
    }

    fun setPageIndex(index: Int) {
        durChapterPos = curTextChapter?.getReadLength(index) ?: index
        saveRead()
        curPageChanged()
    }

    /**
     * 当前页面变化
     */
    private fun curPageChanged() {
        callBack?.pageChanged()
        if (BaseReadAloudService.isRun) {
            ReadAloud.playByEventBus(!BaseReadAloudService.pause)
        }
        upReadTime()
        preDownload()
    }

    /**
     * 朗读
     */
    fun readAloud(play: Boolean = true) {
        book?.let {
            ReadAloud.play(appCtx, play)
        }
    }

    /**
     * 当前页数
     */
    val durPageIndex: Int
        get() {
            return curTextChapter?.getPageIndexByCharIndex(durChapterPos) ?: durChapterPos
        }

    /**
     * chapterOnDur: 0为当前页,1为下一页,-1为上一页
     */
    fun textChapter(chapterOnDur: Int = 0): TextChapter? {
        return when (chapterOnDur) {
            0 -> curTextChapter
            1 -> nextTextChapter
            -1 -> prevTextChapter
            else -> null
        }
    }

    /**
     * 加载当前章节和前后一章内容
     * @param resetPageOffset 滚动阅读是否重置滚动位置
     * @param success 当前章节加载完成回调
     */
    fun loadContent(resetPageOffset: Boolean, success: (() -> Unit)? = null) {
        loadContent(durChapterIndex, resetPageOffset = resetPageOffset) {
            success?.invoke()
        }
        loadContent(durChapterIndex + 1, resetPageOffset = resetPageOffset)
        loadContent(durChapterIndex - 1, resetPageOffset = resetPageOffset)
    }

    /**
     * 加载章节内容
     * @param index 章节序号
     * @param upContent 是否更新视图
     * @param resetPageOffset 滚动阅读是否重置滚动位置
     * @param success 加载完成回调
     */
    fun loadContent(
        index: Int,
        upContent: Boolean = true,
        resetPageOffset: Boolean = false,
        success: (() -> Unit)? = null
    ) {
        if (addLoading(index)) {
            Coroutine.async {
                val book = book!!
                appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                    BookHelp.getContent(book, chapter)?.let {
                        contentLoadFinish(book, chapter, it, upContent, resetPageOffset) {
                            success?.invoke()
                        }
                        removeLoading(chapter.index)
                    } ?: download(this, chapter, resetPageOffset = resetPageOffset)
                } ?: removeLoading(index)
            }.onError {
                removeLoading(index)
                AppLog.put("加载正文出错\n${it.localizedMessage}")
            }
        }
    }

    /**
     * 下载正文
     */
    private fun download(index: Int) {
        if (index < 0) return
        if (index > chapterSize - 1) {
            upToc()
            return
        }
        book?.let { book ->
            if (book.isLocal) return
            if (addLoading(index)) {
                Coroutine.async {
                    appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                        if (BookHelp.hasContent(book, chapter)) {
                            removeLoading(chapter.index)
                        } else {
                            download(this, chapter, false)
                        }
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    /**
     * 下载正文
     */
    private fun download(
        scope: CoroutineScope,
        chapter: BookChapter,
        resetPageOffset: Boolean,
        success: (() -> Unit)? = null
    ) {
        val book = book
        val bookSource = bookSource
        if (book != null && bookSource != null) {
            CacheBook.getOrCreate(bookSource, book).download(scope, chapter)
        } else if (book != null) {
            val msg = if (book.isLocal) "无内容" else "没有书源"
            contentLoadFinish(
                book, chapter, "加载正文失败\n$msg", resetPageOffset = resetPageOffset
            ) {
                success?.invoke()
            }
            removeLoading(chapter.index)
        } else {
            removeLoading(chapter.index)
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
     * 内容加载完成
     */
    fun contentLoadFinish(
        book: Book,
        chapter: BookChapter,
        content: String,
        upContent: Boolean = true,
        resetPageOffset: Boolean,
        success: (() -> Unit)? = null
    ) {
        Coroutine.async {
            removeLoading(chapter.index)
            if (chapter.index in durChapterIndex - 1..durChapterIndex + 1) {
                val contentProcessor = ContentProcessor.get(book.name, book.origin)
                val displayTitle = chapter.getDisplayTitle(
                    contentProcessor.getTitleReplaceRules(),
                    book.getUseReplaceRule()
                )
                val contents = contentProcessor
                    .getContent(book, chapter, content, includeTitle = false)
                val textChapter = ChapterProvider
                    .getTextChapter(book, chapter, displayTitle, contents, chapterSize)
                when (val offset = chapter.index - durChapterIndex) {
                    0 -> {
                        curTextChapter = textChapter
                        if (upContent) callBack?.upContent(offset, resetPageOffset)
                        callBack?.upMenuView()
                        curPageChanged()
                        callBack?.contentLoadFinish()
                    }
                    -1 -> {
                        prevTextChapter = textChapter
                        if (upContent) callBack?.upContent(offset, resetPageOffset)
                    }
                    1 -> {
                        nextTextChapter = textChapter
                        if (upContent) callBack?.upContent(offset, resetPageOffset)
                    }
                }
            }
        }.onError {
            AppLog.put("ChapterProvider ERROR", it)
            appCtx.toastOnUi("ChapterProvider ERROR:\n${it.stackTraceStr}")
        }.onSuccess {
            success?.invoke()
        }
    }

    @Synchronized
    fun upToc() {
        val bookSource = bookSource ?: return
        val book = book ?: return
        if (System.currentTimeMillis() - book.lastCheckTime < 600000) return
        book.lastCheckTime = System.currentTimeMillis()
        WebBook.getChapterList(this, bookSource, book).onSuccess(IO) { cList ->
            if (book.bookUrl == ReadBook.book?.bookUrl
                && cList.size > chapterSize
            ) {
                appDb.bookChapterDao.insert(*cList.toTypedArray())
                chapterSize = cList.size
                nextTextChapter ?: loadContent(1)
            }
        }
    }

    fun pageAnim(): Int {
        return book?.getPageAnim() ?: ReadBookConfig.pageAnim
    }

    fun setCharset(charset: String) {
        book?.let {
            it.charset = charset
            callBack?.loadChapterList(it)
        }
        saveRead()
    }

    fun saveRead() {
        Coroutine.async {
            book?.let { book ->
                book.lastCheckCount = 0
                book.durChapterTime = System.currentTimeMillis()
                book.durChapterIndex = durChapterIndex
                book.durChapterPos = durChapterPos
                appDb.bookChapterDao.getChapter(book.bookUrl, durChapterIndex)?.let {
                    book.durChapterTitle = it.getDisplayTitle(
                        ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
                    )
                }
                appDb.bookDao.update(book)
            }
        }
    }

    /**
     * 预下载
     */
    private fun preDownload() {
        if (AppConfig.preDownloadNum < 2) {
            return
        }
        Coroutine.async {
            //预下载
            val maxChapterIndex = durChapterIndex + AppConfig.preDownloadNum
            for (i in durChapterIndex.plus(2)..maxChapterIndex) {
                delay(1000)
                download(i)
            }
            val minChapterIndex = durChapterIndex - min(5, AppConfig.preDownloadNum)
            for (i in durChapterIndex.minus(2) downTo minChapterIndex) {
                delay(1000)
                download(i)
            }
        }
    }

    interface CallBack {
        fun upMenuView()

        fun loadChapterList(book: Book, callback: (() -> Unit)? = null)

        fun upContent(
            relativePosition: Int = 0,
            resetPageOffset: Boolean = true,
            success: (() -> Unit)? = null
        )

        fun pageChanged()

        fun contentLoadFinish()

        fun upPageAnim()

        fun notifyBookChanged()
    }

}
