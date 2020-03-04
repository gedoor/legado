package io.legado.app.ui.book.read.page

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import io.legado.app.App
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.dp
import io.legado.app.utils.getPrefString
import io.legado.app.utils.removePref


@Suppress("DEPRECATION")
object ChapterProvider {
    var viewWidth = 0
    var viewHeight = 0
    var paddingLeft = 0
    var paddingTop = 0
    var visibleWidth = 0
    var visibleHeight = 0
    var visibleRight = 0
    var visibleBottom = 0
    private var lineSpacingExtra = 0
    private var paragraphSpacing = 0
    var typeface: Typeface = Typeface.SANS_SERIF
    var titlePaint = TextPaint()
    var contentPaint = TextPaint()

    init {
        upStyle()
    }

    /**
     * 获取拆分完的章节数据
     */
    fun getTextChapter(
        bookChapter: BookChapter,
        content: String,
        chapterSize: Int
    ): TextChapter {
        val textPages = arrayListOf<TextPage>()
        val pageLines = arrayListOf<Int>()
        val pageLengths = arrayListOf<Int>()
        val stringBuilder = StringBuilder()
        var surplusText = content
        var durY = 0f
        textPages.add(TextPage())
        while (surplusText.isNotEmpty()) {
            if (textPages.first().textLines.isEmpty()) {
                //title
                val end = surplusText.indexOf("\n")
                if (end > 0) {
                    val title = surplusText.substring(0, end)
                    surplusText = surplusText.substring(end + 1)
                    durY = setTypeText(
                        title, durY, textPages, pageLines, pageLengths, stringBuilder, true
                    )
                }
            } else {
                //正文
                val end = surplusText.indexOf("\n")
                val text: String
                if (end >= 0) {
                    text = surplusText.substring(0, end)
                    surplusText = surplusText.substring(end + 1)
                } else {
                    text = surplusText
                    surplusText = ""
                }
                durY =
                    setTypeText(text, durY, textPages, pageLines, pageLengths, stringBuilder, false)
            }
        }
        textPages.last().height = durY + 20.dp
        textPages.last().text = stringBuilder.toString()
        if (pageLines.size < textPages.size) {
            pageLines.add(textPages.last().textLines.size)
        }
        if (pageLengths.size < textPages.size) {
            pageLengths.add(textPages.last().text.length)
        }
        for ((index, item) in textPages.withIndex()) {
            item.index = index
            item.pageSize = textPages.size
            item.chapterIndex = bookChapter.index
            item.chapterSize = chapterSize
            item.title = bookChapter.title
        }
        return TextChapter(
            bookChapter.index,
            bookChapter.title,
            bookChapter.url,
            textPages,
            pageLines,
            pageLengths,
            chapterSize
        )
    }

    /**
     * 排版文字
     */
    private fun setTypeText(
        text: String,
        y: Float,
        textPages: ArrayList<TextPage>,
        pageLines: ArrayList<Int>,
        pageLengths: ArrayList<Int>,
        stringBuilder: StringBuilder,
        isTitle: Boolean
    ): Float {
        var durY = y
        val textPaint = if (isTitle) titlePaint else contentPaint
        val layout = StaticLayout(
            text, textPaint, visibleWidth,
            Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true
        )
        for (lineIndex in 0 until layout.lineCount) {
            textPages.last().height = durY
            val textLine = TextLine(isTitle = isTitle)
            if (durY + textPaint.textHeight < visibleHeight) {
                textPages.last().textLines.add(textLine)
                durY += textPaint.textHeight + lineSpacingExtra
            } else {
                textPages.last().text = stringBuilder.toString()
                stringBuilder.clear()
                pageLines.add(textPages.last().textLines.size)
                pageLengths.add(textPages.last().text.length)
                //新页面
                durY = textPaint.textHeight + lineSpacingExtra
                textPages.add(TextPage())
                textPages.last().textLines.add(textLine)
            }
            textLine.lineBottom = paddingTop + durY - lineSpacingExtra
            textLine.lineBase = textLine.lineBottom - textPaint.fontMetrics.descent
            textLine.lineTop = textLine.lineBottom - textPaint.textHeight
            val words =
                text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            stringBuilder.append(words)
            textLine.text = words
            val desiredWidth = layout.getLineWidth(lineIndex)
            if (lineIndex == 0 && layout.lineCount > 1 && !isTitle) {
                //第一行
                addCharsToLineFirst(textLine, words, textPaint, desiredWidth)
            } else if (lineIndex == layout.lineCount - 1) {
                //最后一行
                val x = if (isTitle && ReadBookConfig.titleCenter)
                    (visibleWidth - layout.getLineWidth(lineIndex)) / 2
                else 0f
                addCharsToLineLast(textLine, words, stringBuilder, textPaint, x)
            } else {
                //中间行
                addCharsToLineMiddle(textLine, words, textPaint, desiredWidth, 0f)
            }
        }
        durY += paragraphSpacing
        return durY
    }

    /**
     * 有缩进,两端对齐
     */
    private fun addCharsToLineFirst(
        textLine: TextLine,
        words: String,
        textPaint: TextPaint,
        desiredWidth: Float
    ) {
        var x = 0f
        val bodyIndent = ReadBookConfig.bodyIndent
        val icw = StaticLayout.getDesiredWidth(bodyIndent, textPaint) / bodyIndent.length
        for (i in 0..bodyIndent.lastIndex) {
            val x1 = x + icw
            textLine.addTextChar(
                charData = bodyIndent[i].toString(),
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        val words1 = words.replaceFirst(bodyIndent, "")
        addCharsToLineMiddle(textLine, words1, textPaint, desiredWidth, x)
    }

    /**
     * 无缩进,两端对齐
     */
    private fun addCharsToLineMiddle(
        textLine: TextLine,
        words: String,
        textPaint: TextPaint,
        desiredWidth: Float,
        startX: Float
    ) {
        val gapCount: Int = words.length - 1
        val d = (visibleWidth - desiredWidth) / gapCount
        var x = startX
        for (i in words.indices) {
            val char = words[i]
            val cw = StaticLayout.getDesiredWidth(char.toString(), textPaint)
            val x1 = if (i != words.lastIndex) (x + cw + d) else (x + cw)
            textLine.addTextChar(
                charData = char.toString(),
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        exceed(textLine, words)
    }

    /**
     * 最后一行,自然排列
     */
    private fun addCharsToLineLast(
        textLine: TextLine,
        words: String,
        stringBuilder: StringBuilder,
        textPaint: TextPaint,
        startX: Float
    ) {
        stringBuilder.append("\n")
        textLine.text = "$words\n"
        var x = startX
        for (i in words.indices) {
            val char = words[i].toString()
            val cw = StaticLayout.getDesiredWidth(char, textPaint)
            val x1 = x + cw
            textLine.addTextChar(
                charData = char,
                start = paddingLeft + x,
                end = paddingLeft + x1
            )
            x = x1
        }
        exceed(textLine, words)
    }

    /**
     * 超出边界处理
     */
    private fun exceed(textLine: TextLine, words: String) {
        val endX = textLine.textChars.last().end
        if (endX > visibleRight) {
            val cc = (endX - visibleRight) / words.length
            for (i in 0..words.lastIndex) {
                textLine.getTextCharReverseAt(i).let {
                    val py = cc * (words.length - i)
                    it.start = it.start - py
                    it.end = it.end - py
                }
            }
        }
    }

    /**
     * 更新样式
     */
    fun upStyle() {
        typeface = try {
            val fontPath = App.INSTANCE.getPrefString(PreferKey.readBookFont)
            if (!TextUtils.isEmpty(fontPath)) {
                Typeface.createFromFile(fontPath)
            } else {
                when (AppConfig.systemTypefaces) {
                    1 -> Typeface.SERIF
                    2 -> Typeface.MONOSPACE
                    else -> Typeface.SANS_SERIF
                }
            }
        } catch (e: Exception) {
            App.INSTANCE.removePref(PreferKey.readBookFont)
            Typeface.SANS_SERIF
        }
        //标题
        titlePaint.isAntiAlias = true
        titlePaint.color = ReadBookConfig.durConfig.textColor()
        titlePaint.letterSpacing = ReadBookConfig.letterSpacing
        titlePaint.typeface = Typeface.create(typeface, Typeface.BOLD)
        //正文
        contentPaint.isAntiAlias = true
        contentPaint.color = ReadBookConfig.durConfig.textColor()
        contentPaint.letterSpacing = ReadBookConfig.letterSpacing
        val bold = if (ReadBookConfig.textBold) Typeface.BOLD else Typeface.NORMAL
        contentPaint.typeface = Typeface.create(typeface, bold)
        //间距
        lineSpacingExtra = ReadBookConfig.lineSpacingExtra.dp
        paragraphSpacing = ReadBookConfig.paragraphSpacing.dp
        titlePaint.textSize = (ReadBookConfig.textSize + 2).dp.toFloat()
        contentPaint.textSize = ReadBookConfig.textSize.dp.toFloat()

        upSize()
    }

    /**
     * 更新View尺寸
     */
    fun upSize() {
        paddingLeft = ReadBookConfig.paddingLeft.dp
        paddingTop = ReadBookConfig.paddingTop.dp
        visibleWidth = viewWidth - paddingLeft - ReadBookConfig.paddingRight.dp
        visibleHeight = viewHeight - paddingTop - ReadBookConfig.paddingBottom.dp
        visibleRight = paddingLeft + visibleWidth
        visibleBottom = paddingTop + visibleHeight
    }

    private val TextPaint.textHeight: Float
        get() {
            return this.fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading
        }
}