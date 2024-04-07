package io.legado.app.ui.book.read.page.entities


import androidx.annotation.Keep
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.book.BookContent
import io.legado.app.ui.book.read.page.provider.LayoutProgressListener
import io.legado.app.ui.book.read.page.provider.TextChapterLayout
import io.legado.app.utils.fastBinarySearchBy
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.min

/**
 * 章节信息
 */
@Keep
@Suppress("unused")
data class TextChapter(
    val chapter: BookChapter,
    val position: Int,
    val title: String,
    val chaptersSize: Int,
    val sameTitleRemoved: Boolean,
    val isVip: Boolean,
    val isPay: Boolean,
    //起效的替换规则
    val effectiveReplaceRules: List<ReplaceRule>?
) : LayoutProgressListener {

    private val textPages = arrayListOf<TextPage>()
    val pages: List<TextPage> get() = textPages

    private var layout: TextChapterLayout? = null

    val layoutChannel get() = layout!!.channel

    fun getPage(index: Int): TextPage? {
        return pages.getOrNull(index)
    }

    fun getPageByReadPos(readPos: Int): TextPage? {
        return getPage(getPageIndexByCharIndex(readPos))
    }

    val lastPage: TextPage? get() = pages.lastOrNull()

    val lastIndex: Int get() = pages.lastIndex

    val lastReadLength: Int get() = getReadLength(lastIndex)

    val pageSize: Int get() = pages.size

    var listener: LayoutProgressListener? = null

    var isCompleted = false

    val paragraphs by lazy {
        paragraphsInternal
    }

    val pageParagraphs by lazy {
        pageParagraphsInternal
    }

    val paragraphsInternal: ArrayList<TextParagraph>
        get() {
            val paragraphs = arrayListOf<TextParagraph>()
            for (i in pages.indices) {
                val lines = pages[i].lines
                for (a in lines.indices) {
                    val line = lines[a]
                    if (line.paragraphNum <= 0) continue
                    if (paragraphs.lastIndex < line.paragraphNum - 1) {
                        paragraphs.add(TextParagraph(line.paragraphNum))
                    }
                    paragraphs[line.paragraphNum - 1].textLines.add(line)
                }
            }
            return paragraphs
        }

    val pageParagraphsInternal: List<TextParagraph>
        get() {
            val paragraphs = arrayListOf<TextParagraph>()
            for (i in pages.indices) {
                paragraphs.addAll(pages[i].paragraphs)
            }
            for (i in paragraphs.indices) {
                paragraphs[i].num = i + 1
            }
            return paragraphs
        }

    /**
     * @param index 页数
     * @return 是否是最后一页
     */
    fun isLastIndex(index: Int): Boolean {
        return isCompleted && index >= pages.size - 1
    }

    fun isLastIndexCurrent(index: Int): Boolean {
        return index >= pages.size - 1
    }

    /**
     * @param pageIndex 页数
     * @return 已读长度
     */
    fun getReadLength(pageIndex: Int): Int {
        if (pageIndex < 0) return 0
        return pages[min(pageIndex, lastIndex)].lines.first().chapterPosition
        /*
        var length = 0
        val maxIndex = min(pageIndex, pages.size)
        for (index in 0 until maxIndex) {
            length += pages[index].charSize
        }
        return length
        */
    }

    /**
     * @param length 当前页面文字在章节中的位置
     * @return 下一页位置,如果没有下一页返回-1
     */
    fun getNextPageLength(length: Int): Int {
        val pageIndex = getPageIndexByCharIndex(length)
        if (pageIndex + 1 >= pageSize) {
            return -1
        }
        return getReadLength(pageIndex + 1)
    }

    /**
     * @param length 当前页面文字在章节中的位置
     * @return 上一页位置,如果没有上一页返回-1
     */
    fun getPrevPageLength(length: Int): Int {
        val pageIndex = getPageIndexByCharIndex(length)
        if (pageIndex - 1 < 0) {
            return -1
        }
        return getReadLength(pageIndex - 1)
    }

    /**
     * 获取内容
     */
    fun getContent(): String {
        val stringBuilder = StringBuilder()
        pages.forEach {
            stringBuilder.append(it.text)
        }
        return stringBuilder.toString()
    }

    /**
     * @return 获取未读文字
     */
    fun getUnRead(pageIndex: Int): String {
        val stringBuilder = StringBuilder()
        if (pages.isNotEmpty()) {
            for (index in pageIndex..pages.lastIndex) {
                stringBuilder.append(pages[index].text)
            }
        }
        return stringBuilder.toString()
    }

    /**
     * @return 需要朗读的文本列表
     * @param pageIndex 起始页
     * @param pageSplit 是否分页
     * @param startPos 从当前页什么地方开始朗读
     */
    fun getNeedReadAloud(pageIndex: Int, pageSplit: Boolean, startPos: Int): String {
        val stringBuilder = StringBuilder()
        if (pages.isNotEmpty()) {
            for (index in pageIndex..pages.lastIndex) {
                stringBuilder.append(pages[index].text)
                if (pageSplit && !stringBuilder.endsWith("\n")) {
                    stringBuilder.append("\n")
                }
            }
        }
        return stringBuilder.substring(startPos).toString()
    }

    fun getParagraphNum(
        position: Int,
        pageSplit: Boolean,
    ): Int {
        val paragraphs = getParagraphs(pageSplit)
        paragraphs.forEach { paragraph ->
            if (position in paragraph.chapterIndices) {
                return paragraph.num
            }
        }
        return -1
    }

    fun getParagraphs(pageSplit: Boolean): List<TextParagraph> {
        return if (pageSplit) {
            if (isCompleted) pageParagraphs else pageParagraphsInternal
        } else {
            if (isCompleted) paragraphs else paragraphsInternal
        }
    }

    fun getLastParagraphPosition(): Int {
        return pageParagraphs.last().chapterPosition
    }

    /**
     * @return 根据索引位置获取所在页
     */
    fun getPageIndexByCharIndex(charIndex: Int): Int {
        val pageSize = pages.size
        if (pageSize == 0) {
            return -1
        }
        val bIndex = pages.fastBinarySearchBy(charIndex, 0, pageSize) {
            it.lines.first().chapterPosition
        }
        val index = abs(bIndex + 1) - 1
        // 判断是否已经排版到 charIndex ，没有则返回 -1
        if (!isCompleted && index == pageSize - 1) {
            val page = pages[index]
            val line = page.lines.first()
            val pageEndPos = line.chapterPosition + page.charSize
            if (charIndex > pageEndPos) {
                return -1
            }
        }
        return index
        /*
        var length = 0
        for (i in pages.indices) {
            val page = pages[i]
            length += page.charSize
            if (length > charIndex) {
                return page.index
            }
        }
        return pages.lastIndex
        */
    }

    fun clearSearchResult() {
        for (i in pages.indices) {
            val page = pages[i]
            page.searchResult.forEach {
                it.selected = false
                it.isSearchResult = false
            }
            page.searchResult.clear()
        }
    }

    fun createLayout(scope: CoroutineScope, book: Book, bookContent: BookContent) {
        if (layout != null) {
            throw IllegalStateException("已经排版过了")
        }
        layout = TextChapterLayout(
            scope,
            this,
            textPages,
            book,
            bookContent,
        )
    }

    fun setProgressListener(l: LayoutProgressListener?) {
        if (isCompleted) {
            // no op
        } else if (layout?.exception != null) {
            l?.onLayoutException(layout?.exception!!)
        } else {
            listener = l
        }
    }

    override fun onLayoutPageCompleted(index: Int, page: TextPage) {
        listener?.onLayoutPageCompleted(index, page)
    }

    override fun onLayoutCompleted() {
        isCompleted = true
        listener?.onLayoutCompleted()
        listener = null
    }

    override fun onLayoutException(e: Throwable) {
        listener?.onLayoutException(e)
        listener = null
    }

    fun cancelLayout() {
        layout?.cancel()
        listener = null
    }

    companion object {
        val emptyTextChapter = TextChapter(
            BookChapter(), -1, "emptyTextChapter", -1,
            sameTitleRemoved = false,
            isVip = false,
            isPay = false,
            null
        ).apply { isCompleted = true }
    }

}
