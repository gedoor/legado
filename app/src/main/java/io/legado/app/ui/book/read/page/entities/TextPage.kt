package io.legado.app.ui.book.read.page.entities

import android.text.Layout
import android.text.StaticLayout
import io.legado.app.App
import io.legado.app.R
import io.legado.app.ui.book.read.page.ChapterProvider

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

    @Suppress("DEPRECATION")
    fun format(): TextPage {
        if (textLines.isEmpty() && ChapterProvider.visibleWidth > 0) {
            val layout = StaticLayout(
                text, ChapterProvider.contentPaint, ChapterProvider.visibleWidth,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
            )
            var y = (ChapterProvider.visibleHeight - layout.height) / 2f
            if (y < 0) y = 0f
            for (lineIndex in 0 until layout.lineCount) {
                val textLine = TextLine()
                textLine.lineTop = (ChapterProvider.paddingTop + y -
                        (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)))
                textLine.lineBase = (ChapterProvider.paddingTop + y -
                        (layout.getLineBottom(lineIndex) - layout.getLineBaseline(lineIndex)))
                textLine.lineBottom =
                    textLine.lineBase + ChapterProvider.contentPaint.fontMetrics.descent
                var x = (ChapterProvider.visibleWidth - layout.getLineMax(lineIndex)) / 2
                textLine.text =
                    text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
                for (i in textLine.text.indices) {
                    val char = textLine.text[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, ChapterProvider.contentPaint)
                    val x1 = x + cw
                    textLine.textChars.add(TextChar(charData = char, start = x, end = x1))
                    x = x1
                }
                textLines.add(textLine)
            }
            height = ChapterProvider.visibleHeight
        }
        return this
    }

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
