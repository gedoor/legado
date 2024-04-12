package io.legado.app.ui.book.read.page.entities

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import androidx.annotation.Keep
import androidx.core.graphics.withTranslation
import io.legado.app.R
import io.legado.app.help.PaintPool
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.entities.TextChapter.Companion.emptyTextChapter
import io.legado.app.ui.book.read.page.entities.column.TextColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.canvasrecorder.CanvasRecorderFactory
import io.legado.app.utils.canvasrecorder.recordIfNeeded
import io.legado.app.utils.dpToPx
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
    var chapterSize: Int = 0,
    var chapterIndex: Int = 0,
    var height: Float = 0f,
    var leftLineSize: Int = 0
) {

    companion object {
        val readProgressFormatter = DecimalFormat("0.0%")
        val emptyTextPage = TextPage()
    }

    val lines: List<TextLine> get() = textLines
    val lineSize: Int get() = textLines.size
    val charSize: Int get() = text.length.coerceAtLeast(1)
    val searchResult = hashSetOf<TextColumn>()
    var isMsgPage: Boolean = false
    var canvasRecorder = CanvasRecorderFactory.create(true)
    var doublePage = false
    var paddingTop = ChapterProvider.paddingTop
    var isCompleted = false
    var hasReadAloudSpan = false

    @JvmField
    var textChapter = emptyTextChapter
    val pageSize get() = textChapter.pageSize

    val paragraphs by lazy {
        paragraphsInternal
    }

    val paragraphsInternal: ArrayList<TextParagraph>
        get() {
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
        line.textPage = this
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
            val pageHeight = lastLine.lineBottom + contentPaintTextHeight * lineSpacingExtra
            if (visibleHeight - pageHeight >= lastLineHeight) return@run
            val surplus = (visibleBottom - lastLine.lineBottom)
            if (surplus == 0f) return@run
            height += surplus
            val tj = surplus / (leftLineSize - 1)
            for (i in 1 until leftLineSize) {
                val line = textLines[i]
                line.lineTop += tj * i
                line.lineBase += tj * i
                line.lineBottom += tj * i
            }
        }
        if (leftLineSize == lineSize) return
        ChapterProvider.run {
            val lastLine = textLines.last()
            if (lastLine.isImage) return@run
            val lastLineHeight = with(lastLine) { lineBottom - lineTop }
            val pageHeight = lastLine.lineBottom + contentPaintTextHeight * lineSpacingExtra
            if (visibleHeight - pageHeight >= lastLineHeight) return@run
            val surplus = (visibleBottom - lastLine.lineBottom)
            if (surplus == 0f) return@run
            val tj = surplus / (textLines.size - leftLineSize - 1)
            for (i in leftLineSize + 1 until textLines.size) {
                val line = textLines[i]
                val surplusIndex = i - leftLineSize
                line.lineTop += tj * surplusIndex
                line.lineBase += tj * surplusIndex
                line.lineBottom += tj * surplusIndex
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
                addLine(textLine)
            }
            height = ChapterProvider.visibleHeight.toFloat()
            invalidate()
            isCompleted = true
        }
        return this
    }

    /**
     * 移除朗读标志
     */
    fun removePageAloudSpan(): TextPage {
        if (!hasReadAloudSpan) {
            return this
        }
        hasReadAloudSpan = false
        for (i in textLines.indices) {
            textLines[i].isReadAloud = false
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
        for (index in textLines.indices) {
            val textLine = textLines[index]
            val lineLength = textLine.text.length + if (textLine.isParagraphEnd) 1 else 0
            if (aloudSpanStart >= lineStart && aloudSpanStart < lineStart + lineLength) {
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
            val df = readProgressFormatter
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
        val columns = textLines[maxIndex].columns
        for (index in 0 until columnIndex) {
            val column = columns[index]
            if (column is TextColumn) {
                length += column.charData.length
            }
        }
        return length
    }

    /**
     * @return 页面所在章节
     */
    fun getTextChapter(): TextChapter {
        return textChapter
    }

    /**
     * 判断章节字符位置是否在这一页中
     *
     * @param chapterPos 章节字符位置
     * @return
     */
    fun containPos(chapterPos: Int): Boolean {
        val line = lines.first()
        val startPos = line.chapterPosition
        val endPos = startPos + charSize
        return chapterPos in startPos..<endPos
    }

    fun draw(view: ContentTextView, canvas: Canvas, relativeOffset: Float) {
        if (AppConfig.optimizeRender) {
            render(view)
            canvas.withTranslation(0f, relativeOffset + paddingTop) {
                canvasRecorder.draw(this)
            }
        } else {
            canvas.withTranslation(0f, relativeOffset + paddingTop) {
                drawPage(view, this)
            }
        }
    }

    private fun drawDebugInfo(canvas: Canvas) {
        ChapterProvider.run {
            val paint = PaintPool.obtain()
            paint.style = Paint.Style.STROKE
            canvas.drawRect(
                paddingLeft.toFloat(),
                0f,
                (paddingLeft + visibleWidth).toFloat(),
                height - 1.dpToPx(),
                paint
            )
            PaintPool.recycle(paint)
        }
    }

    private fun drawPage(view: ContentTextView, canvas: Canvas) {
        for (i in lines.indices) {
            val line = lines[i]
            canvas.withTranslation(0f, line.lineTop - paddingTop) {
                line.draw(view, this)
            }
        }
    }

    fun render(view: ContentTextView): Boolean {
        if (!isCompleted) return false
        return canvasRecorder.recordIfNeeded(view.width, height.toInt()) {
            drawPage(view, this)
        }
    }

    fun invalidate() {
        canvasRecorder.invalidate()
    }

    fun invalidateAll() {
        for (i in lines.indices) {
            lines[i].invalidateSelf()
        }
        invalidate()
    }

    fun recycleRecorders() {
        canvasRecorder.recycle()
        for (i in lines.indices) {
            lines[i].recycleRecorder()
        }
    }

    fun hasImageOrEmpty(): Boolean {
        return textLines.any { it.isImage } || textLines.isEmpty()
    }
}
