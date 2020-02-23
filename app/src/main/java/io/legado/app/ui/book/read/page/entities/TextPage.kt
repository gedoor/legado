package io.legado.app.ui.book.read.page.entities

import io.legado.app.App
import io.legado.app.R

data class TextPage(
    var index: Int = 0,
    var text: String = App.INSTANCE.getString(R.string.data_loading),
    var title: String = "",
    val textLines: ArrayList<TextLine> = arrayListOf(),
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0,
    var height: Int = 0
) {

    fun removePageAloudSpan(): TextPage {
        textLines.forEach { textLine ->
            textLine.isReadAloud = false
        }
        return this
    }

    fun upPageAloudSpan(pageStart: Int) {
        removePageAloudSpan()
        var lineStart = 0
        for ((index, textLine) in textLines.withIndex()) {
            if (pageStart > lineStart && pageStart < lineStart + textLine.text.length) {
                for (i in index - 1 downTo 0) {
                    if (textLines[i].text.endsWith("\n")) {
                        break
                    } else {
                        textLines[i].isReadAloud = true
                    }
                }
                for (i in index until textLines.size) {
                    if (textLines[i].text.endsWith("\n")) {
                        textLines[i].isReadAloud = true
                        break
                    } else {
                        textLines[i].isReadAloud = true
                    }
                }
                break
            }
            lineStart += textLine.text.length
        }
    }
}
