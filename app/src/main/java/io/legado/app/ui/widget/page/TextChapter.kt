package io.legado.app.ui.widget.page

import android.text.SpannableStringBuilder

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
            val spannableStringBuilder = SpannableStringBuilder()
            pages.forEach {
                spannableStringBuilder.append(it.text)
            }
            return TextPage(0, spannableStringBuilder, title, position, chaptersSize)
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
        for (index in 0 until pageIndex) {
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
}

