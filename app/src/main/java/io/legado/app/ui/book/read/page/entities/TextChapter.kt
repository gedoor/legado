package io.legado.app.ui.book.read.page.entities

import kotlin.math.min

data class TextChapter(
    val position: Int,
    val title: String,
    val url: String,
    val pages: List<TextPage>,
    val pageLines: List<Int>,
    val pageLengths: List<Int>,
    val chaptersSize: Int
) {
    fun page(index: Int): TextPage? {
        if (index >= 0 && index < pages.size) {
            return pages[index]
        }
        return null
    }

    fun lastPage(): TextPage? {
        if (pages.isNotEmpty()) {
            return pages[pages.lastIndex]
        }
        return null
    }

    fun scrollPage(): TextPage? {
        if (pages.isNotEmpty()) {
            val stringBuilder = StringBuilder()
            pages.forEach {
                stringBuilder.append(it.text)
            }
            return TextPage(
                index = 0, text = stringBuilder.toString(), title = title,
                pageSize = pages.size, chapterSize = chaptersSize, chapterIndex = position
            )
        }
        return null
    }

    fun lastIndex(): Int {
        return pages.size - 1
    }

    fun isLastIndex(index: Int): Boolean {
        return index >= pages.size - 1
    }

    fun pageSize(): Int {
        return pages.size
    }

    fun getReadLength(pageIndex: Int): Int {
        var length = 0
        val maxIndex = min(pageIndex, pages.size)
        for (index in 0 until maxIndex) {
            length += pageLengths[index]
        }
        return length
    }

    fun getUnRead(pageIndex: Int): String {
        val stringBuilder = StringBuilder()
        if (pageIndex < pages.size && pages.isNotEmpty()) {
            for (index in pageIndex..lastIndex()) {
                stringBuilder.append(pages[index].text)
            }
        }
        return stringBuilder.toString()
    }

    fun getStartLine(pageIndex: Int): Int {
        if (pageLines.size > pageIndex) {
            var lines = 0
            for (index: Int in 0 until pageIndex) {
                lines += pageLines[index] + 1
            }
            return lines
        }
        return 0
    }

    fun getPageIndex(line: Int): Int {
        var lines = 0
        for (pageIndex in pageLines.indices) {
            lines += pageLines[pageIndex] + 1
            if (line < lines) {
                return pageIndex
            }
        }
        return 0
    }

    fun getContent(): String {
        val stringBuilder = StringBuilder()
        pages.forEach {
            stringBuilder.append(it.text)
        }
        return stringBuilder.toString()
    }
}

