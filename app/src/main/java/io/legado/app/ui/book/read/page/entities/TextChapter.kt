package io.legado.app.ui.book.read.page.entities

import kotlin.math.min

data class TextChapter(
    val position: Int,
    val title: String,
    val url: String,
    val pages: List<TextPage>,
    val chaptersSize: Int
) {

    fun page(index: Int): TextPage? {
        return pages.getOrNull(index)
    }

    fun getPageByReadPos(readPos: Int): TextPage? {
        return page(getPageIndexByCharIndex(readPos))
    }

    val lastPage: TextPage? get() = pages.lastOrNull()

    val lastIndex: Int get() = pages.lastIndex

    val lastReadLength: Int get() = getReadLength(lastIndex)

    val pageSize: Int get() = pages.size

    fun isLastIndex(index: Int): Boolean {
        return index >= pages.size - 1
    }

    fun getReadLength(pageIndex: Int): Int {
        var length = 0
        val maxIndex = min(pageIndex, pages.size)
        for (index in 0 until maxIndex) {
            length += pages[index].charSize
        }
        return length
    }

    fun getNextPageLength(length: Int): Int {
        return getReadLength(getPageIndexByCharIndex(length) + 1)
    }

    fun getUnRead(pageIndex: Int): String {
        val stringBuilder = StringBuilder()
        if (pages.isNotEmpty()) {
            for (index in pageIndex..pages.lastIndex) {
                stringBuilder.append(pages[index].text)
            }
        }
        return stringBuilder.toString()
    }

    fun getContent(): String {
        val stringBuilder = StringBuilder()
        pages.forEach {
            stringBuilder.append(it.text)
        }
        return stringBuilder.toString()
    }

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
}