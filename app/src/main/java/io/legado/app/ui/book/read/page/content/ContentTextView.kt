package io.legado.app.ui.book.read.page.content

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import io.legado.app.ui.book.read.page.ChapterProvider
import io.legado.app.ui.book.read.page.entities.TextPage


class ContentTextView(context: Context, attrs: AttributeSet?) :
    BaseContentTextView(context, attrs) {

    fun setContent(textPage: TextPage) {
        this.textPage = textPage
        contentDescription = textPage.text
        invalidate()
    }

    override fun drawHorizontalPage(canvas: Canvas) {
        textPage.textLines.forEach { textLine ->
            drawChars(
                canvas,
                textLine.textChars,
                textLine.lineTop,
                textLine.lineBase,
                textLine.lineBottom,
                textLine.isTitle,
                textLine.isReadAloud
            )
        }
    }

    override fun drawScrollPage(canvas: Canvas) {
        val mPageOffset = pageOffset
        textPage.textLines.forEach { textLine ->
            val lineTop = textLine.lineTop + mPageOffset
            val lineBase = textLine.lineBase + mPageOffset
            val lineBottom = textLine.lineBottom + mPageOffset
            drawChars(
                canvas,
                textLine.textChars,
                lineTop,
                lineBase,
                lineBottom,
                textLine.isTitle,
                textLine.isReadAloud
            )
        }
        pageFactory.nextPage?.textLines?.forEach { textLine ->
            val yPy = mPageOffset + textPage.height - ChapterProvider.paddingTop
            val lineTop = textLine.lineTop + yPy
            val lineBase = textLine.lineBase + yPy
            val lineBottom = textLine.lineBottom + yPy
            drawChars(
                canvas,
                textLine.textChars,
                lineTop,
                lineBase,
                lineBottom,
                textLine.isTitle,
                textLine.isReadAloud
            )
        }
    }

    fun onScroll(mOffset: Float) {
        if (mOffset == 0f) return
        var offset = mOffset
        if (offset > maxScrollOffset) {
            offset = maxScrollOffset
        } else if (offset < -maxScrollOffset) {
            offset = -maxScrollOffset
        }

        pageOffset += offset
        if (pageOffset > 0) {
            pageFactory.moveToPrev()
            textPage = pageFactory.currentPage ?: TextPage().format()
            pageOffset -= textPage.height
            upView?.invoke(textPage)
        } else if (pageOffset < -textPage.height) {
            pageOffset += textPage.height
            pageFactory.moveToNext()
            textPage = pageFactory.currentPage ?: TextPage().format()
            upView?.invoke(textPage)
        }
        invalidate()
    }

    fun resetPageOffset() {
        pageOffset = 0f
    }

    fun selectText(x: Float, y: Float, select: (lineIndex: Int, charIndex: Int) -> Unit) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        textChar.selected = true
                        invalidate()
                        selectLineStart = lineIndex
                        selectCharStart = charIndex
                        selectLineEnd = lineIndex
                        selectCharEnd = charIndex
                        upSelectedStart(textChar.start, textLine.lineBottom)
                        upSelectedEnd(textChar.end, textLine.lineBottom)
                        select(lineIndex, charIndex)
                    }
                }
                break
            }
        }
    }

    fun selectStartMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineStart != lineIndex || selectCharStart != charIndex) {
                            selectLineStart = lineIndex
                            selectCharStart = charIndex
                            upSelectedStart(textChar.start, textLine.lineBottom)
                            upSelectChars(textPage)
                        }
                        break
                    }
                }
                break
            }
        }
    }

    fun selectStartMoveIndex(lineIndex: Int, charIndex: Int) {
        selectLineStart = lineIndex
        selectCharStart = charIndex
        val textLine = textPage.textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedStart(textChar.start, textLine.lineBottom)
        upSelectChars(textPage)
    }

    fun selectEndMove(x: Float, y: Float) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            if (y > textLine.lineTop && y < textLine.lineBottom) {
                for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                    if (x > textChar.start && x < textChar.end) {
                        if (selectLineEnd != lineIndex || selectCharEnd != charIndex) {
                            selectLineEnd = lineIndex
                            selectCharEnd = charIndex
                            upSelectedEnd(textChar.end, textLine.lineBottom)
                            upSelectChars(textPage)
                        }
                        break
                    }
                }
                break
            }
        }
    }

    fun selectEndMoveIndex(lineIndex: Int, charIndex: Int) {
        selectLineEnd = lineIndex
        selectCharEnd = charIndex
        val textLine = textPage.textLines[lineIndex]
        val textChar = textLine.textChars[charIndex]
        upSelectedEnd(textChar.end, textLine.lineBottom)
        upSelectChars(textPage)
    }

    private fun upSelectChars(textPage: TextPage) {
        for ((lineIndex, textLine) in textPage.textLines.withIndex()) {
            for ((charIndex, textChar) in textLine.textChars.withIndex()) {
                textChar.selected =
                    if (lineIndex == selectLineStart && lineIndex == selectLineEnd) {
                        charIndex in selectCharStart..selectCharEnd
                    } else if (lineIndex == selectLineStart) {
                        charIndex >= selectCharStart
                    } else if (lineIndex == selectLineEnd) {
                        charIndex <= selectCharEnd
                    } else {
                        lineIndex in (selectLineStart + 1) until selectLineEnd
                    }
            }
        }
        invalidate()
    }

    fun cancelSelect() {
        textPage.textLines.forEach { textLine ->
            textLine.textChars.forEach {
                it.selected = false
            }
        }
        invalidate()
        callBack.onCancelSelect()
    }

    val selectedText: String
        get() {
            val stringBuilder = StringBuilder()
            for (lineIndex in selectLineStart..selectLineEnd) {
                if (lineIndex == selectLineStart && lineIndex == selectLineEnd) {
                    stringBuilder.append(
                        textPage.textLines[lineIndex].text.substring(
                            selectCharStart,
                            selectCharEnd + 1
                        )
                    )
                } else if (lineIndex == selectLineStart) {
                    stringBuilder.append(
                        textPage.textLines[lineIndex].text.substring(
                            selectCharStart
                        )
                    )
                } else if (lineIndex == selectLineEnd) {
                    stringBuilder.append(
                        textPage.textLines[lineIndex].text.substring(0, selectCharEnd + 1)
                    )
                } else {
                    stringBuilder.append(textPage.textLines[lineIndex].text)
                }
            }
            return stringBuilder.toString()
        }

}
