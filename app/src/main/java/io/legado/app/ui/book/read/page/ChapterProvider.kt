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
import io.legado.app.help.BookHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextChar
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
    private var lineSpacingExtra = 0f
    private var paragraphSpacing = 0
    var typeface: Typeface = Typeface.SANS_SERIF
    var titlePaint = TextPaint()
    var contentPaint = TextPaint()
    private var bodyIndent = BookHelp.bodyIndent

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
        var durY = 0
        textPages.add(TextPage())
        while (surplusText.isNotEmpty()) {
            if (textPages.first().textLines.isEmpty()) {
                //title
                val end = surplusText.indexOf("\n")
                if (end > 0) {
                    val title = surplusText.substring(0, end)
                    surplusText = surplusText.substring(end + 1)
                    durY = joinTitle(title, durY, textPages, pageLines, pageLengths, stringBuilder)
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
                durY = joinBody(text, durY, textPages, pageLines, pageLengths, stringBuilder)
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
     * 标题
     */
    private fun joinTitle(
        title: String,
        y: Int,
        textPages: ArrayList<TextPage>,
        pageLines: ArrayList<Int>,
        pageLengths: ArrayList<Int>,
        stringBuilder: StringBuilder
    ): Int {
        var durY = y
        val layout = StaticLayout(
            title, titlePaint, visibleWidth,
            Layout.Alignment.ALIGN_NORMAL, 1f, lineSpacingExtra, true
        )
        for (lineIndex in 0 until layout.lineCount) {
            textPages.last().height = durY
            durY = durY + layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
            val textLine = TextLine(isTitle = true)
            if (durY < visibleHeight) {
                textPages.last().textLines.add(textLine)
            } else {
                textPages.last().text = stringBuilder.toString()
                stringBuilder.clear()
                pageLines.add(textPages.last().textLines.size)
                pageLengths.add(textPages.last().text.length)
                //新页面
                durY = layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
                textPages.add(TextPage())
                textPages.last().textLines.add(textLine)
            }
            textLine.lineTop = (paddingTop + durY -
                    (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex))).toFloat()
            textLine.lineBase = (paddingTop + durY -
                    (layout.getLineBottom(lineIndex) - layout.getLineBaseline(lineIndex))).toFloat()
            textLine.lineBottom = textLine.lineBase + titlePaint.fontMetrics.descent
            val words =
                title.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            stringBuilder.append(words)
            textLine.text = words
            val desiredWidth = layout.getLineMax(lineIndex)
            if (lineIndex != layout.lineCount - 1) {
                val gapCount: Int = words.length - 1
                val d = (visibleWidth - desiredWidth) / gapCount
                var x = 0f
                for (i in words.indices) {
                    val char = words[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, titlePaint)
                    val x1 = if (i != words.lastIndex) (x + cw + d) else (x + cw)
                    val textChar = TextChar(
                        charData = char,
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar)
                    x = x1
                }
            } else {
                //最后一行
                textLine.text = "$words\n"
                stringBuilder.append("\n")
                var x = if (ReadBookConfig.titleCenter)
                    (visibleWidth - layout.getLineMax(lineIndex)) / 2
                else 0f
                for (i in words.indices) {
                    val char = words[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, titlePaint)
                    val x1 = x + cw
                    val textChar = TextChar(
                        charData = char,
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar)
                    x = x1
                }
            }
        }
        durY += paragraphSpacing
        return durY
    }

    /**
     * 正文
     */
    private fun joinBody(
        text: String,
        y: Int,
        textPages: ArrayList<TextPage>,
        pageLines: ArrayList<Int>,
        pageLengths: ArrayList<Int>,
        stringBuilder: StringBuilder
    ): Int {
        var durY = y
        val layout = StaticLayout(
            text, contentPaint, visibleWidth,
            Layout.Alignment.ALIGN_NORMAL, 1f, lineSpacingExtra, true
        )
        for (lineIndex in 0 until layout.lineCount) {
            textPages.last().height = durY
            durY = durY + layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
            val textLine = TextLine()
            if (durY < visibleHeight) {
                textPages.last().textLines.add(textLine)
            } else {
                textPages.last().text = stringBuilder.toString()
                stringBuilder.clear()
                pageLines.add(textPages.last().textLines.size)
                pageLengths.add(textPages.last().text.length)
                //新页面
                durY = layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
                textPages.add(TextPage())
                textPages.last().textLines.add(textLine)
            }
            textLine.lineTop = (paddingTop + durY -
                    (layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex))).toFloat()
            textLine.lineBase = (paddingTop + durY -
                    (layout.getLineBottom(lineIndex) - layout.getLineBaseline(lineIndex))).toFloat()
            textLine.lineBottom = textLine.lineBase + contentPaint.fontMetrics.descent
            var words =
                text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            stringBuilder.append(words)
            textLine.text = words
            val desiredWidth = layout.getLineMax(lineIndex)
            if (lineIndex == 0 && layout.lineCount > 1) {
                //第一行
                var x = 0f
                val icw = StaticLayout.getDesiredWidth(bodyIndent, contentPaint) / bodyIndent.length
                for (i in 0..bodyIndent.lastIndex) {
                    val x1 = x + icw
                    val textChar = TextChar(
                        charData = bodyIndent[i].toString(),
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar)
                    x = x1
                }
                words = words.replaceFirst(bodyIndent, "")
                val gapCount: Int = words.length - 1
                val d = (visibleWidth - desiredWidth) / gapCount
                for (i in words.indices) {
                    val char = words[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                    val x1 = if (i != words.lastIndex) x + cw + d else x + cw
                    val textChar1 = TextChar(
                        charData = char,
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar1)
                    x = x1
                }
            } else if (lineIndex == layout.lineCount - 1) {
                //最后一行
                stringBuilder.append("\n")
                textLine.text = "$words\n"
                var x = 0f
                for (i in words.indices) {
                    val char = words[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                    val x1 = x + cw
                    val textChar = TextChar(
                        charData = char,
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar)
                    x = x1
                }
            } else {
                //中间行
                val gapCount: Int = words.length - 1
                val d = (visibleWidth - desiredWidth) / gapCount
                var x = 0f
                for (i in words.indices) {
                    val char = words[i].toString()
                    val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                    val x1 = if (i != words.lastIndex) x + cw + d else x + cw
                    val textChar = TextChar(
                        charData = char,
                        start = paddingLeft + x,
                        end = paddingLeft + x1
                    )
                    textLine.textChars.add(textChar)
                    x = x1
                }
            }
        }
        durY += paragraphSpacing
        return durY
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
        lineSpacingExtra = ReadBookConfig.lineSpacingExtra.dp.toFloat()
        paragraphSpacing = ReadBookConfig.paragraphSpacing.dp
        titlePaint.textSize = (ReadBookConfig.textSize + 2).dp.toFloat()
        contentPaint.textSize = ReadBookConfig.textSize.dp.toFloat()

        bodyIndent = BookHelp.bodyIndent

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

}