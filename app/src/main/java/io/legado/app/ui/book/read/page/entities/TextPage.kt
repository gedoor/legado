package io.legado.app.ui.book.read.page.entities

import android.text.Layout
import android.text.StaticLayout
import io.legado.app.R
import io.legado.app.help.ReadBookConfig
import io.legado.app.model.BookRead
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import splitties.init.appCtx
import java.text.DecimalFormat
import kotlin.math.min

@Suppress("unused", "MemberVisibilityCanBePrivate")
data class TextPage(
    var index: Int = 0,
    var text: String = appCtx.getString(R.string.data_loading),
    var title: String = "",
    val textLines: ArrayList<TextLine> = arrayListOf(),
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0,
    var height: Float = 0f,
    var leftLineSize: Int = 0
) {

    val lineSize get() = textLines.size
    val charSize get() = text.length

    fun getLine(index: Int): TextLine {
        return textLines.getOrElse(index) {
            textLines.last()
        }
    }

    fun upLinesPosition() {
        if (!ReadBookConfig.textBottomJustify) return
        if (textLines.size <= 1) return
        if (leftLineSize == 0) {
            leftLineSize = lineSize
        }
        ChapterProvider.run {
            val lastLine = textLines[leftLineSize - 1]
            if (lastLine.isImage) return@run
            val lastLineHeight = with(lastLine) { lineBottom - lineTop }
            if (visibleHeight - lastLine.lineBottom >= lastLineHeight) return@run
            val surplus = (visibleBottom - lastLine.lineBottom)
            if (surplus == 0f) return@run
            height += surplus
            val tj = surplus / (leftLineSize - 1)
            for (i in 1 until leftLineSize) {
                val line = textLines[i]
                line.lineTop = line.lineTop + tj * i
                line.lineBase = line.lineBase + tj * i
                line.lineBottom = line.lineBottom + tj * i
            }
        }
        if (leftLineSize == lineSize) return
        ChapterProvider.run {
            val lastLine = textLines.last()
            if (lastLine.isImage) return@run
            val lastLineHeight = with(lastLine) { lineBottom - lineTop }
            if (visibleHeight - lastLine.lineBottom >= lastLineHeight) return@run
            val surplus = (visibleBottom - lastLine.lineBottom)
            if (surplus == 0f) return@run
            val tj = surplus / (textLines.size - leftLineSize - 1)
            for (i in leftLineSize + 1 until textLines.size) {
                val line = textLines[i]
                val surplusIndex = i - leftLineSize
                line.lineTop = line.lineTop + tj * surplusIndex
                line.lineBase = line.lineBase + tj * surplusIndex
                line.lineBottom = line.lineBottom + tj * surplusIndex
            }
        }
    }

    @Suppress("DEPRECATION")
    fun format(): TextPage {
        if (textLines.isEmpty() && ChapterProvider.viewWidth > 0) {
            val visibleWidth = ChapterProvider.visibleRight - ChapterProvider.paddingLeft
            val layout = StaticLayout(
                text, ChapterProvider.contentPaint, visibleWidth,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
            )
            var y = (ChapterProvider.visibleHeight - layout.height) / 2f
            if (y < 0) y = 0f
            for (lineIndex in 0 until layout.lineCount) {
                val textLine = TextLine()
                textLine.lineTop = ChapterProvider.paddingTop + y + layout.getLineTop(lineIndex)
                textLine.lineBase =
                    ChapterProvider.paddingTop + y + layout.getLineBaseline(lineIndex)
                textLine.lineBottom =
                    ChapterProvider.paddingTop + y + layout.getLineBottom(lineIndex)
                var x = ChapterProvider.paddingLeft +
                        (visibleWidth - layout.getLineMax(lineIndex)) / 2
                textLine.text =
                    text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
                for (i in textLine.text.indices) {
                    val char = textLine.text[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, ChapterProvider.contentPaint)
                    val x1 = x + cw
                    textLine.textChars.add(
                        TextChar(
                            char, start = x, end = x1
                        )
                    )
                    x = x1
                }
                textLines.add(textLine)
            }
            height = ChapterProvider.visibleHeight.toFloat()
        }
        return this
    }

    fun removePageAloudSpan(): TextPage {
        textLines.forEach { textLine ->
            textLine.isReadAloud = false
        }
        return this
    }

    fun upPageAloudSpan(aloudSpanStart: Int) {
        removePageAloudSpan()
        var lineStart = 0
        for ((index, textLine) in textLines.withIndex()) {
            if (aloudSpanStart > lineStart && aloudSpanStart < lineStart + textLine.text.length) {
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

    val readProgress: String
        get() {
            val df = DecimalFormat("0.0%")
            if (chapterSize == 0 || pageSize == 0 && chapterIndex == 0) {
                return "0.0%"
            } else if (pageSize == 0) {
                return df.format((chapterIndex + 1.0f) / chapterSize.toDouble())
            }
            var percent =
                df.format(chapterIndex * 1.0f / chapterSize + 1.0f / chapterSize * (index + 1) / pageSize.toDouble())
            if (percent == "100.0%" && (chapterIndex + 1 != chapterSize || index + 1 != pageSize)) {
                percent = "99.9%"
            }
            return percent
        }

    fun getSelectStartLength(lineIndex: Int, charIndex: Int): Int {
        var length = 0
        val maxIndex = min(lineIndex, lineSize)
        for (index in 0 until maxIndex) {
            length += textLines[index].charSize
        }
        return length + charIndex
    }

    fun getTextChapter(): TextChapter? {
        BookRead.curTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        BookRead.nextTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        BookRead.prevTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        return null
    }
}
