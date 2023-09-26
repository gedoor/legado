package io.legado.app.ui.book.read.page.entities


import androidx.annotation.Keep
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.ReplaceRule
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
    val pages: List<TextPage>,
    val chaptersSize: Int,
    val sameTitleRemoved: Boolean,
    val isVip: Boolean,
    val isPay: Boolean,
    //起效的替换规则
    val effectiveReplaceRules: List<ReplaceRule>?
) {

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

    val paragraphs by lazy {
        paragraphsInternal
    }

    val pageParagraphs by lazy {
        pageParagraphsInternal
    }

    val paragraphsInternal: ArrayList<TextParagraph>
        get() {
            val paragraphs = arrayListOf<TextParagraph>()
            pages.forEach {
                it.lines.forEach loop@{ line ->
                    if (line.paragraphNum <= 0) return@loop
                    if (paragraphs.lastIndex < line.paragraphNum - 1) {
                        paragraphs.add(TextParagraph(line.paragraphNum))
                    }
                    paragraphs[line.paragraphNum - 1].textLines.add(line)
                }
            }
            return paragraphs
        }

    val pageParagraphsInternal: List<TextParagraph>
        get() = pages.map {
            it.paragraphs
        }.flatten().also {
            it.forEachIndexed { index, textParagraph ->
                textParagraph.num = index + 1
            }
        }

    /**
     * @param index 页数
     * @return 是否是最后一页
     */
    fun isLastIndex(index: Int): Boolean {
        return index >= pages.size - 1
    }

    /**
     * @param pageIndex 页数
     * @return 已读长度
     */
    fun getReadLength(pageIndex: Int): Int {
        var length = 0
        val maxIndex = min(pageIndex, pages.size)
        for (index in 0 until maxIndex) {
            length += pages[index].charSize
        }
        return length
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
        val paragraphs = if (pageSplit) {
            pageParagraphs
        } else {
            paragraphs
        }
        paragraphs.forEach { paragraph ->
            if (position in paragraph.chapterIndices) {
                return paragraph.num
            }
        }
        return -1
    }

    fun getLastParagraphPosition(): Int {
        return pageParagraphs.last().chapterPosition
    }

    /**
     * @return 根据索引位置获取所在页
     */
    fun getPageIndexByCharIndex(charIndex: Int): Int {
        var length = 0
        pages.forEach {
            length += it.charSize
            if (length > charIndex) {
                return it.index
            }
        }
        return pages.lastIndex
    }

    fun clearSearchResult() {
        pages.forEach { page ->
            page.searchResult.forEach {
                it.selected = false
                it.isSearchResult = false
            }
            page.searchResult.clear()
        }
    }

}
