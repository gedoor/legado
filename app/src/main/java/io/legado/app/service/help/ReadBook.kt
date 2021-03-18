package io.legado.app.service.help

import androidx.lifecycle.MutableLiveData
import com.github.liuyueyi.quick.transfer.ChineseUtils
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.*
import io.legado.app.help.*
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.BookWebDav
import io.legado.app.model.webBook.WebBook
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.book.read.page.provider.ImageProvider
import io.legado.app.utils.msg
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.init.appCtx


@Suppress("MemberVisibilityCanBePrivate")
object ReadBook {
    var titleDate = MutableLiveData<String>()
    var book: Book? = null
    var contentProcessor: ContentProcessor? = null
    var inBookshelf = false
    var chapterSize = 0
    var durChapterIndex = 0
    var durChapterPos = 0
    var isLocalBook = true
    var callBack: CallBack? = null
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    var bookSource: BookSource? = null
    var webBook: WebBook? = null
    var msg: String? = null
    private val loadingChapters = arrayListOf<Int>()
    private val readRecord = ReadRecord()
    var readStartTime: Long = System.currentTimeMillis()

    fun resetData(book: Book) {
        this.book = book
        contentProcessor = ContentProcessor(book.name, book.origin)
        readRecord.bookName = book.name
        readRecord.readTime = appDb.readRecordDao.getReadTime(book.name) ?: 0
        durChapterIndex = book.durChapterIndex
        durChapterPos = book.durChapterPos
        isLocalBook = book.origin == BookType.local
        chapterSize = book.totalChapterNum
        clearTextChapter()
        titleDate.postValue(book.name)
        callBack?.upPageAnim()
        upWebBook(book)
        ImageProvider.clearAllCache()
        synchronized(this) {
            loadingChapters.clear()
        }
    }

    fun upWebBook(book: Book) {
        if (book.origin == BookType.local) {
            bookSource = null
            webBook = null
        } else {
            appDb.bookSourceDao.getBookSource(book.origin)?.let {
                bookSource = it
                webBook = WebBook(it)
                if (book.getImageStyle().isNullOrBlank()) {
                    book.setImageStyle(it.getContentRule().imageStyle)
                }
            } ?: let {
                bookSource = null
                webBook = null
            }
        }
    }

    fun setProgress(progress: BookProgress) {
        durChapterIndex = progress.durChapterIndex
        durChapterPos = progress.durChapterPos
        clearTextChapter()
        loadContent(resetPageOffset = true)
    }

    fun clearTextChapter() {
        prevTextChapter = null
        curTextChapter = null
        nextTextChapter = null
    }

    fun uploadProgress(syncBookProgress: Boolean = AppConfig.syncBookProgress) {
        if (syncBookProgress) {
            book?.let {
                BookWebDav.uploadBookProgress(it)
            }
        }
    }

    fun upReadStartTime() {
        Coroutine.async {
            readRecord.readTime = readRecord.readTime + System.currentTimeMillis() - readStartTime
            readStartTime = System.currentTimeMillis()
            appDb.readRecordDao.insert(readRecord)
        }
    }

    fun upMsg(msg: String?) {
        if (this.msg != msg) {
            this.msg = msg
            callBack?.upContent()
        }
    }

    fun moveToNextPage() {
        durChapterPos = curTextChapter?.getNextPageLength(durChapterPos) ?: durChapterPos
        callBack?.upContent()
        saveRead()
    }

    fun moveToNextChapter(upContent: Boolean): Boolean {
        if (durChapterIndex < chapterSize - 1) {
            durChapterPos = 0
            durChapterIndex++
            prevTextChapter = curTextChapter
            curTextChapter = nextTextChapter
            nextTextChapter = null
            book?.let {
                if (curTextChapter == null) {
                    loadContent(durChapterIndex, upContent, false)
                } else if (upContent) {
                    callBack?.upContent()
                }
                loadContent(durChapterIndex.plus(1), upContent, false)
                if (AppConfig.preDownload) {
                    GlobalScope.launch(Dispatchers.IO) {
                        for (i in 2..9) {
                            delay(1000)
                            download(durChapterIndex + i)
                        }
                    }
                }
            }
            saveRead()
            callBack?.upView()
            curPageChanged()
            return true
        } else {
            return false
        }
    }

    fun moveToPrevChapter(upContent: Boolean, toLast: Boolean = true): Boolean {
        if (durChapterIndex > 0) {
            durChapterPos = if (toLast) prevTextChapter?.lastReadLength ?: 0 else 0
            durChapterIndex--
            nextTextChapter = curTextChapter
            curTextChapter = prevTextChapter
            prevTextChapter = null
            book?.let {
                if (curTextChapter == null) {
                    loadContent(durChapterIndex, upContent, false)
                } else if (upContent) {
                    callBack?.upContent()
                }
                loadContent(durChapterIndex.minus(1), upContent, false)
                if (AppConfig.preDownload) {
                    GlobalScope.launch(Dispatchers.IO) {
                        for (i in 2..9) {
                            delay(1000)
                            download(durChapterIndex - i)
                        }
                    }
                }
            }
            saveRead()
            callBack?.upView()
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

    private fun curPageChanged() {
        callBack?.pageChanged()
        if (BaseReadAloudService.isRun) {
            readAloud(!BaseReadAloudService.pause)
        }
        upReadStartTime()
    }

    /**
     * 朗读
     */
    fun readAloud(play: Boolean = true) {
        val book = book
        val textChapter = curTextChapter
        if (book != null && textChapter != null) {
            val key = IntentDataHelp.putData(textChapter)
            ReadAloud.play(
                appCtx, book.name, textChapter.title, durPageIndex(), key, play
            )
        }
    }

    fun durPageIndex(): Int {
        curTextChapter?.let {
            return it.getPageIndexByCharIndex(durChapterPos)
        }
        return durChapterPos
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
     * 加载章节内容
     */
    fun loadContent(resetPageOffset: Boolean, success: (() -> Unit)? = null) {
        loadContent(durChapterIndex, resetPageOffset = resetPageOffset) {
            success?.invoke()
        }
        loadContent(durChapterIndex + 1, resetPageOffset = resetPageOffset)
        loadContent(durChapterIndex - 1, resetPageOffset = resetPageOffset)
    }

    fun loadContent(
        index: Int,
        upContent: Boolean = true,
        resetPageOffset: Boolean,
        success: (() -> Unit)? = null
    ) {
        book?.let { book ->
            if (addLoading(index)) {
                Coroutine.async {
                    appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                        BookHelp.getContent(book, chapter)?.let {
                            contentLoadFinish(book, chapter, it, upContent, resetPageOffset) {
                                success?.invoke()
                            }
                            removeLoading(chapter.index)
                        } ?: download(chapter, resetPageOffset = resetPageOffset)
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    private fun download(index: Int) {
        book?.let { book ->
            if (book.isLocalBook()) return
            if (addLoading(index)) {
                Coroutine.async {
                    appDb.bookChapterDao.getChapter(book.bookUrl, index)?.let { chapter ->
                        if (BookHelp.hasContent(book, chapter)) {
                            removeLoading(chapter.index)
                        } else {
                            download(chapter, false)
                        }
                    } ?: removeLoading(index)
                }.onError {
                    removeLoading(index)
                }
            }
        }
    }

    private fun download(
        chapter: BookChapter,
        resetPageOffset: Boolean,
        success: (() -> Unit)? = null
    ) {
        val book = book
        val webBook = webBook
        if (book != null && webBook != null) {
            CacheBook.download(Coroutine.DEFAULT, webBook, book, chapter)
        } else if (book != null) {
            contentLoadFinish(
                book, chapter, "没有书源", resetPageOffset = resetPageOffset
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

    fun searchResultPositions(
        pages: List<TextPage>,
        indexWithinChapter: Int,
        query: String
    ): Array<Int> {
        // calculate search result's pageIndex
        var content = ""
        pages.map {
            content += it.text
        }
        var count = 1
        var index = content.indexOf(query)
        while (count != indexWithinChapter) {
            index = content.indexOf(query, index + 1)
            count += 1
        }
        val contentPosition = index
        var pageIndex = 0
        var length = pages[pageIndex].text.length
        while (length < contentPosition) {
            pageIndex += 1
            if (pageIndex > pages.size) {
                pageIndex = pages.size
                break
            }
            length += pages[pageIndex].text.length
        }

        // calculate search result's lineIndex
        val currentPage = pages[pageIndex]
        var lineIndex = 0
        length = length - currentPage.text.length + currentPage.textLines[lineIndex].text.length
        while (length < contentPosition) {
            lineIndex += 1
            if (lineIndex > currentPage.textLines.size) {
                lineIndex = currentPage.textLines.size
                break
            }
            length += currentPage.textLines[lineIndex].text.length
        }

        // charIndex
        val currentLine = currentPage.textLines[lineIndex]
        length -= currentLine.text.length
        val charIndex = contentPosition - length
        var addLine = 0
        var charIndex2 = 0
        // change line
        if ((charIndex + query.length) > currentLine.text.length) {
            addLine = 1
            charIndex2 = charIndex + query.length - currentLine.text.length - 1
        }
        // changePage
        if ((lineIndex + addLine + 1) > currentPage.textLines.size) {
            addLine = -1
            charIndex2 = charIndex + query.length - currentLine.text.length - 1
        }
        return arrayOf(pageIndex, lineIndex, charIndex, addLine, charIndex2)
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
            ImageProvider.clearOut(durChapterIndex)
            if (chapter.index in durChapterIndex - 1..durChapterIndex + 1) {
                chapter.title = when (AppConfig.chineseConverterType) {
                    1 -> ChineseUtils.t2s(chapter.title)
                    2 -> ChineseUtils.s2t(chapter.title)
                    else -> chapter.title
                }
                val contents = contentProcessor!!.getContent(book, chapter.title, content)
                when (chapter.index) {
                    durChapterIndex -> {
                        curTextChapter =
                            ChapterProvider.getTextChapter(
                                book, chapter, contents, chapterSize
                            )
                        if (upContent) callBack?.upContent(resetPageOffset = resetPageOffset)
                        callBack?.upView()
                        curPageChanged()
                        callBack?.contentLoadFinish()
                    }
                    durChapterIndex - 1 -> {
                        prevTextChapter =
                            ChapterProvider.getTextChapter(
                                book, chapter, contents, chapterSize
                            )
                        if (upContent) callBack?.upContent(-1, resetPageOffset)
                    }
                    durChapterIndex + 1 -> {
                        nextTextChapter =
                            ChapterProvider.getTextChapter(
                                book, chapter, contents, chapterSize
                            )
                        if (upContent) callBack?.upContent(1, resetPageOffset)
                    }
                }
            }
        }.onError {
            it.printStackTrace()
            appCtx.toastOnUi("ChapterProvider ERROR:\n${it.msg}")
        }.onSuccess {
            success?.invoke()
        }
    }

    fun pageAnim(): Int {
        book?.let {
            return if (it.getPageAnim() < 0)
                ReadBookConfig.pageAnim
            else
                it.getPageAnim()
        }
        return ReadBookConfig.pageAnim
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
                    book.durChapterTitle = it.title
                }
                appDb.bookDao.update(book)
            }
        }
    }

    interface CallBack {
        fun loadChapterList(book: Book)

        fun upContent(
            relativePosition: Int = 0,
            resetPageOffset: Boolean = true,
            success: (() -> Unit)? = null
        )

        fun upView()

        fun pageChanged()

        fun contentLoadFinish()

        fun upPageAnim()
    }

}