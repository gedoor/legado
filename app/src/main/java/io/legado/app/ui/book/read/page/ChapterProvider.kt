package io.legado.app.ui.book.read.page

import android.graphics.Point
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.BookHelp
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextChar
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.dp


object ChapterProvider {
    var viewWidth = 0
    var viewHeight = 0
    private var visibleWidth = 0
    private var visibleHeight = 0
    private var paddingLeft = 0
    private var paddingTop = 0
    private var lineSpacingExtra = 0
    private var paragraphSpacing = 0
    var titlePaint = TextPaint()
    var contentPaint = TextPaint()

    init {
        upStyle(ReadBookConfig.durConfig)
    }

    fun upStyle(config: ReadBookConfig.Config) {
        titlePaint.color = config.textColor()
        titlePaint.letterSpacing = config.letterSpacing
        contentPaint.color = config.textColor()
        contentPaint.letterSpacing = config.letterSpacing
        lineSpacingExtra = config.lineSpacingExtra
        paragraphSpacing = config.paragraphSpacing
        titlePaint.textSize = (config.textSize + 2).dp.toFloat()
        contentPaint.textSize = config.textSize.dp.toFloat()
        upSize(config)
    }

    fun upSize(config: ReadBookConfig.Config) {
        paddingLeft = config.paddingLeft.dp
        paddingTop = config.paddingTop.dp
        visibleWidth = viewWidth - paddingLeft - config.paddingRight.dp
        visibleHeight = viewHeight - paddingTop - config.paddingBottom.dp
    }

    @Suppress("DEPRECATION")
    fun getTextChapter(
        bookChapter: BookChapter,
        content: String,
        chapterSize: Int,
        isHtml: Boolean = false
    ): TextChapter {
        val bodyIndent = BookHelp.bodyIndent
        val textPages = arrayListOf<TextPage>()
        val pageLines = arrayListOf<Int>()
        val pageLengths = arrayListOf<Int>()
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
                    val layout = StaticLayout(
                        title, titlePaint, visibleWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1f, lineSpacingExtra.toFloat(), false
                    )
                    for (lineIndex in 0 until layout.lineCount) {
                        durY = durY + layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
                        val textLine = TextLine(isTitle = true)
                        if (durY < visibleHeight) {
                            textPages.last().textLines.add(textLine)
                        } else {
                            durY = 0
                            textPages.add(TextPage())
                            textPages.last().textLines.add(textLine)
                        }
                        textLine.lineBottom = layout.getLineBottom(lineIndex)
                        textLine.lineTop = layout.getLineTop(lineIndex)
                        val words = title.substring(
                            layout.getLineStart(lineIndex),
                            layout.getLineEnd(lineIndex)
                        )
                        val desiredWidth = layout.getLineMax(lineIndex)
                        if (lineIndex != layout.lineCount - 1) {
                            val gapCount: Int = words.length - 1
                            val d = (visibleWidth - desiredWidth) / gapCount
                            var x = 0
                            for (i in words.indices) {
                                val char = words[i].toString()
                                val cw = StaticLayout.getDesiredWidth(char, titlePaint)
                                val x1 = if (i != words.lastIndex) {
                                    (x + cw + d).toInt()
                                } else {
                                    (x + cw).toInt()
                                }
                                val textChar = TextChar(
                                    charData = char,
                                    leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                                    rightTopPosition = Point(
                                        paddingLeft + x1,
                                        paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                                    )
                                )
                                textLine.textChars.add(textChar)
                                x = x1
                            }
                        } else {
                            //最后一行
                            var x = 0
                            for (i in words.indices) {
                                val char = words[i].toString()
                                val cw = StaticLayout.getDesiredWidth(char, titlePaint)
                                val x1 = (x + cw).toInt()
                                val textChar = TextChar(
                                    charData = char,
                                    leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                                    rightTopPosition = Point(
                                        paddingLeft + x1,
                                        paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                                    )
                                )
                                textLine.textChars.add(textChar)
                                x = x1
                            }
                        }
                    }
                    durY += paragraphSpacing
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
                val layout = StaticLayout(
                    text, contentPaint, visibleWidth,
                    Layout.Alignment.ALIGN_NORMAL, 1f, lineSpacingExtra.toFloat(), false
                )
                for (lineIndex in 0 until layout.lineCount) {
                    val textLine = TextLine(isTitle = false)
                    durY = durY + layout.getLineBottom(lineIndex) - layout.getLineTop(lineIndex)
                    if (durY < visibleHeight) {
                        textPages.last().textLines.add(textLine)
                    } else {
                        durY = 0
                        textPages.add(TextPage())
                        textPages.last().textLines.add(textLine)
                    }
                    textLine.lineBottom = layout.getLineBottom(lineIndex)
                    textLine.lineTop = layout.getLineTop(lineIndex)
                    var words =
                        text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
                    val desiredWidth = layout.getLineMax(lineIndex)
                    if (lineIndex == 0 && layout.lineCount > 1) {
                        //第一行
                        var x = 0
                        val icw = StaticLayout.getDesiredWidth(bodyIndent, contentPaint)
                        var x1 = (x + icw).toInt()
                        val textChar = TextChar(
                            charData = bodyIndent,
                            leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                            rightTopPosition = Point(
                                paddingLeft + x1,
                                paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                            )
                        )
                        textLine.textChars.add(textChar)
                        x = x1
                        words = words.replaceFirst(bodyIndent, "")
                        val gapCount: Int = words.length - 1
                        val d = (visibleWidth - desiredWidth) / gapCount
                        for (i in words.indices) {
                            val char = words[i].toString()
                            val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                            x1 = if (i != words.lastIndex) {
                                (x + cw + d).toInt()
                            } else {
                                (x + cw).toInt()
                            }
                            val textChar1 = TextChar(
                                charData = char,
                                leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                                rightTopPosition = Point(
                                    paddingLeft + x1,
                                    paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                                )
                            )
                            textLine.textChars.add(textChar1)
                            x = x1
                        }
                    } else if (lineIndex == layout.lineCount - 1) {
                        //最后一行
                        var x = 0
                        for (i in words.indices) {
                            val char = words[i].toString()
                            val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                            val x1 = (x + cw).toInt()
                            val textChar = TextChar(
                                charData = char,
                                leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                                rightTopPosition = Point(
                                    paddingLeft + x1,
                                    paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                                )
                            )
                            textLine.textChars.add(textChar)
                            x = x1
                        }
                    } else {
                        //中间行
                        val gapCount: Int = words.length - 1
                        val d = (visibleWidth - desiredWidth) / gapCount
                        var x = 0
                        for (i in words.indices) {
                            val char = words[i].toString()
                            val cw = StaticLayout.getDesiredWidth(char, contentPaint)
                            val x1 = if (i != words.lastIndex) {
                                (x + cw + d).toInt()
                            } else {
                                (x + cw).toInt()
                            }
                            val textChar = TextChar(
                                charData = char,
                                leftBottomPosition = Point(paddingLeft + x, paddingTop + durY),
                                rightTopPosition = Point(
                                    paddingLeft + x1,
                                    paddingTop + durY - (textLine.lineBottom - textLine.lineTop)
                                )
                            )
                            textLine.textChars.add(textChar)
                            x = x1
                        }
                    }
                }
                durY += paragraphSpacing
            }
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
}