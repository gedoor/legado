package io.legado.app.ui.book.read.page.entities

import android.text.Layout
import android.text.StaticLayout
import androidx.annotation.Keep
import io.legado.app.R
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.entities.column.TextColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.textHeight
import splitties.init.appCtx
import java.text.DecimalFormat
import kotlin.math.min

/**
 * 页面信息
 */
@Keep
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class TextPage(
    var index: Int = 0,
    var text: String = appCtx.getString(R.string.data_loading),
    var title: String = appCtx.getString(R.string.data_loading),
    private val textLines: ArrayList<TextLine> = arrayListOf(),
    var pageSize: Int = 0,
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0,
    var height: Float = 0f,
    var leftLineSize: Int = 0
) {

    val lines: List<TextLine> get() = textLines
    val lineSize: Int get() = textLines.size
    val charSize: Int get() = text.length
    val searchResult = hashSetOf<TextColumn>()
    var isMsgPage: Boolean = false

    val paragraphs by lazy {
        paragraphsInternal
    }

    val paragraphsInternal: ArrayList<TextParagraph> get() {
        val paragraphs = arrayListOf<TextParagraph>()
        val lines = textLines.filter { it.paragraphNum > 0 }
        val offset = lines.first().paragraphNum - 1
        lines.forEach { line ->
            if (paragraphs.lastIndex < line.paragraphNum - offset - 1) {
                paragraphs.add(TextParagraph(0))
            }
            paragraphs[line.paragraphNum - offset - 1].textLines.add(line)
        }
        return paragraphs
    }

    fun addLine(line: TextLine) {
        textLines.add(line)
    }

    fun getLine(index: Int): TextLine {
        return textLines.getOrElse(index) {
            textLines.last()
        }
    }

    /**
     * 底部对齐更新行位置
     */
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
            val pageHeight = lastLine.lineBottom + contentPaint.textHeight * lineSpacingExtra
            if (visibleHeight - pageHeight >= lastLineHeight) return@run
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
            val pageHeight = lastLine.lineBottom + contentPaint.textHeight * lineSpacingExtra
            if (visibleHeight - pageHeight >= lastLineHeight) return@run
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

    /**
     * 计算文字位置,只用作单页面内容
     */
    @Suppress("DEPRECATION")
    fun format(): TextPage {
        if (textLines.isEmpty()) isMsgPage = true
        if (isMsgPage && ChapterProvider.viewWidth > 0) {
            textLines.clear()
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
                    textLine.addColumn(
                        TextColumn(start = x, end = x1, char)
                    )
                    x = x1
                }
                textLines.add(textLine)
            }
            height = ChapterProvider.visibleHeight.toFloat()
        }
        return this
    }

    /**
     * 移除朗读标志
     */
    fun removePageAloudSpan(): TextPage {
        textLines.forEach { textLine ->
            textLine.isReadAloud = false
        }
        return this
    }

    /**
     * 更新朗读标志
     * @param aloudSpanStart 朗读文字开始位置
     */
    fun upPageAloudSpan(aloudSpanStart: Int) {
        removePageAloudSpan()
        var lineStart = 0
        for ((index, textLine) in textLines.withIndex()) {
            val lineLength = textLine.text.length + if (textLine.isParagraphEnd) 1 else 0
            if (aloudSpanStart > lineStart && aloudSpanStart < lineStart + lineLength) {
                for (i in index - 1 downTo 0) {
                    if (textLines[i].isParagraphEnd) {
                        break
                    } else {
                        textLines[i].isReadAloud = true
                    }
                }
                for (i in index until textLines.size) {
                    if (textLines[i].isParagraphEnd) {
                        textLines[i].isReadAloud = true
                        break
                    } else {
                        textLines[i].isReadAloud = true
                    }
                }
                break
            }
            lineStart += lineLength
        }
    }

    /**
     * 阅读进度
     */
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

    /**
     * 根据行和列返回字符在本页的位置
     * @param lineIndex 字符在第几行
     * @param columnIndex 字符在第几列
     * @return 字符在本页位置
     */
    fun getPosByLineColumn(lineIndex: Int, columnIndex: Int): Int {
        var length = 0
        val maxIndex = min(lineIndex, lineSize)
        for (index in 0 until maxIndex) {
            length += textLines[index].charSize
            if (textLines[index].isParagraphEnd) {
                length++
            }
        }
        return length + columnIndex
    }

    /**
     * @return 页面所在章节
     */
    fun getTextChapter(): TextChapter? {
        ReadBook.curTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        ReadBook.nextTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        ReadBook.prevTextChapter?.let {
            if (it.position == chapterIndex) {
                return it
            }
        }
        return null
    }

    fun hasImageOrEmpty(): Boolean {
        return textLines.any { it.isImage } || textLines.isEmpty()
    }
}
