package io.legado.app.ui.book.read.page.provider

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookContent
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.model.ImageProvider
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.entities.column.ImageColumn
import io.legado.app.ui.book.read.page.entities.column.ReviewColumn
import io.legado.app.ui.book.read.page.entities.column.TextColumn
import io.legado.app.utils.dpToPx
import io.legado.app.utils.fastSum
import io.legado.app.utils.splitNotBlank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Locale

class TextChapterLayout(
    scope: CoroutineScope,
    private val textChapter: TextChapter,
    private val textPages: ArrayList<TextPage>,
    private val book: Book,
    private val bookContent: BookContent,
) {

    @Volatile
    private var listener: LayoutProgressListener? = textChapter

    private val paddingLeft = ChapterProvider.paddingLeft
    private val paddingTop = ChapterProvider.paddingTop

    private val titlePaint = ChapterProvider.titlePaint
    private val titlePaintTextHeight = ChapterProvider.titlePaintTextHeight
    private val titlePaintFontMetrics = ChapterProvider.titlePaintFontMetrics

    private val contentPaint = ChapterProvider.contentPaint
    private val contentPaintTextHeight = ChapterProvider.contentPaintTextHeight
    private val contentPaintFontMetrics = ChapterProvider.contentPaintFontMetrics

    private val titleTopSpacing = ChapterProvider.titleTopSpacing
    private val titleBottomSpacing = ChapterProvider.titleBottomSpacing
    private val lineSpacingExtra = ChapterProvider.lineSpacingExtra
    private val paragraphSpacing = ChapterProvider.paragraphSpacing

    private val visibleHeight = ChapterProvider.visibleHeight
    private val visibleWidth = ChapterProvider.visibleWidth

    private val viewWidth = ChapterProvider.viewWidth
    private val doublePage = ChapterProvider.doublePage
    private val indentCharWidth = ChapterProvider.indentCharWidth
    private val stringBuilder = StringBuilder()

    private var isCompleted = false
    private var exception: Throwable? = null
    private val job: Coroutine<*>
    private val bookChapter inline get() = textChapter.chapter
    private val displayTitle inline get() = textChapter.title
    private val chaptersSize inline get() = textChapter.chaptersSize


    init {
        job = Coroutine.async(scope) {
            launch {
                val bookSource = book.getBookSource() ?: return@launch
                BookHelp.saveImages(bookSource, book, bookChapter, bookContent.toString())
            }
            getTextChapter(book, bookChapter, displayTitle, bookContent)
        }.onError {
            exception = it
            onException(it)
        }.onFinally {
            isCompleted = true
        }
    }

    fun setProgressListener(l: LayoutProgressListener) {
        try {
            if (isCompleted) {
                l.onLayoutCompleted()
            } else if (exception != null) {
                l.onLayoutException(exception!!)
            } else {
                listener = l
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppLog.put("调用布局进度监听回调出错\n${e.localizedMessage}", e)
        }
    }

    fun cancel() {
        job.cancel()
    }

    private fun onPageCompleted() {
        val textPage = textPages.last()
        textPage.index = textPages.lastIndex
        textPage.chapterIndex = bookChapter.index
        textPage.chapterSize = chaptersSize
        textPage.title = displayTitle
        textPage.doublePage = doublePage
        textPage.paddingTop = paddingTop
        textPage.isCompleted = true
        textPage.textChapter = textChapter
        textPage.upLinesPosition()
        try {
            listener?.onLayoutPageCompleted(textPages.lastIndex, textPage)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLog.put("调用布局进度监听回调出错\n${e.localizedMessage}", e)
        }
    }

    private fun onCompleted() {
        try {
            listener?.onLayoutCompleted()
        } catch (e: Exception) {
            e.printStackTrace()
            AppLog.put("调用布局进度监听回调出错\n${e.localizedMessage}", e)
        } finally {
            listener = null
        }
    }

    private fun onException(e: Throwable) {
        if (e is CancellationException) {
            return
        }
        try {
            listener?.onLayoutException(e)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLog.put("调用布局进度监听回调出错\n${e.localizedMessage}", e)
        } finally {
            listener = null
        }
    }

    /**
     * 获取拆分完的章节数据
     */
    private suspend fun getTextChapter(
        book: Book,
        bookChapter: BookChapter,
        displayTitle: String,
        bookContent: BookContent,
    ) {
        val contents = bookContent.textList
        var absStartX = paddingLeft
        var durY = 0f
        textPages.add(TextPage())
        if (ReadBookConfig.titleMode != 2 || bookChapter.isVolume) {
            //标题非隐藏
            displayTitle.splitNotBlank("\n").forEach { text ->
                setTypeText(
                    book, absStartX, durY,
                    if (AppConfig.enableReview) text + ChapterProvider.reviewChar else text,
                    titlePaint,
                    titlePaintTextHeight,
                    titlePaintFontMetrics,
                    isTitle = true,
                    emptyContent = contents.isEmpty(),
                    isVolumeTitle = bookChapter.isVolume
                ).let {
                    absStartX = it.first
                    durY = it.second
                }
            }
            textPages.last().lines.last().isParagraphEnd = true
            stringBuilder.append("\n")
            durY += titleBottomSpacing
        }
        val sb = StringBuffer()
        contents.forEach { content ->
            if (book.getImageStyle().equals(Book.imgStyleText, true)) {
                //图片样式为文字嵌入类型
                var text = content.replace(ChapterProvider.srcReplaceChar, "▣")
                val srcList = LinkedList<String>()
                sb.setLength(0)
                val matcher = AppPattern.imgPattern.matcher(text)
                while (matcher.find()) {
                    matcher.group(1)?.let { src ->
                        srcList.add(src)
                        matcher.appendReplacement(sb, ChapterProvider.srcReplaceChar)
                    }
                }
                matcher.appendTail(sb)
                text = sb.toString()
                setTypeText(
                    book,
                    absStartX,
                    durY,
                    text,
                    contentPaint,
                    contentPaintTextHeight,
                    contentPaintFontMetrics,
                    srcList = srcList
                ).let {
                    absStartX = it.first
                    durY = it.second
                }
            } else {
                val matcher = AppPattern.imgPattern.matcher(content)
                var start = 0
                while (matcher.find()) {
                    val text = content.substring(start, matcher.start())
                    if (text.isNotBlank()) {
                        setTypeText(
                            book,
                            absStartX,
                            durY,
                            text,
                            contentPaint,
                            contentPaintTextHeight,
                            contentPaintFontMetrics
                        ).let {
                            absStartX = it.first
                            durY = it.second
                        }
                    }
                    setTypeImage(
                        book,
                        matcher.group(1)!!,
                        absStartX,
                        durY,
                        textPages,
                        contentPaintTextHeight,
                        stringBuilder,
                        book.getImageStyle()
                    ).let {
                        absStartX = it.first
                        durY = it.second
                    }
                    start = matcher.end()
                }
                if (start < content.length) {
                    val text = content.substring(start, content.length)
                    if (text.isNotBlank()) {
                        setTypeText(
                            book, absStartX, durY,
                            if (AppConfig.enableReview) text + ChapterProvider.reviewChar else text,
                            contentPaint,
                            contentPaintTextHeight,
                            contentPaintFontMetrics
                        ).let {
                            absStartX = it.first
                            durY = it.second
                        }
                    }
                }
            }
            textPages.last().lines.last().isParagraphEnd = true
            stringBuilder.append("\n")
        }
        val textPage = textPages.last()
        val endPadding = 20.dpToPx()
        val durYPadding = durY + endPadding
        if (textPage.height < durYPadding) {
            textPage.height = durYPadding
        } else {
            textPage.height += endPadding
        }
        textPage.text = stringBuilder.toString()
        onPageCompleted()
        onCompleted()
    }

    /**
     * 排版图片
     */
    private suspend fun setTypeImage(
        book: Book,
        src: String,
        x: Int,
        y: Float,
        textPages: ArrayList<TextPage>,
        textHeight: Float,
        stringBuilder: StringBuilder,
        imageStyle: String?,
    ): Pair<Int, Float> {
        var absStartX = x
        var durY = y
        val size = ImageProvider.getImageSize(book, src, ReadBook.bookSource)
        if (size.width > 0 && size.height > 0) {
            if (durY > visibleHeight) {
                val textPage = textPages.last()
                if (textPage.height < durY) {
                    textPage.height = durY
                }
                textPage.text = stringBuilder.toString().ifEmpty { "本页无文字内容" }
                stringBuilder.clear()
                onPageCompleted()
                textPages.add(TextPage())
                durY = 0f
            }
            var height = size.height
            var width = size.width
            when (imageStyle?.uppercase(Locale.ROOT)) {
                Book.imgStyleFull -> {
                    width = visibleWidth
                    height = size.height * visibleWidth / size.width
                }

                else -> {
                    if (size.width > visibleWidth) {
                        height = size.height * visibleWidth / size.width
                        width = visibleWidth
                    }
                    if (height > visibleHeight) {
                        width = width * visibleHeight / height
                        height = visibleHeight
                    }
                    if (durY + height > visibleHeight) {
                        val textPage = textPages.last()
                        if (doublePage && absStartX < viewWidth / 2) {
                            //当前页面左列结束
                            textPage.leftLineSize = textPage.lineSize
                            absStartX = viewWidth / 2 + paddingLeft
                        } else {
                            //当前页面结束
                            if (textPage.leftLineSize == 0) {
                                textPage.leftLineSize = textPage.lineSize
                            }
                            textPage.text = stringBuilder.toString().ifEmpty { "本页无文字内容" }
                            stringBuilder.clear()
                            onPageCompleted()
                            textPages.add(TextPage())
                        }
                        // 双页的 durY 不正确，可能会小于实际高度
                        if (textPage.height < durY) {
                            textPage.height = durY
                        }
                        durY = 0f
                    }
                }
            }
            val textLine = TextLine(isImage = true)
            textLine.lineTop = durY + paddingTop
            durY += height
            textLine.lineBottom = durY + paddingTop
            val (start, end) = if (visibleWidth > width) {
                val adjustWidth = (visibleWidth - width) / 2f
                Pair(adjustWidth, adjustWidth + width)
            } else {
                Pair(0f, width.toFloat())
            }
            textLine.addColumn(
                ImageColumn(start = x + start, end = x + end, src = src)
            )
            calcTextLinePosition(textPages, textLine, stringBuilder.length)
            stringBuilder.append(" ") // 确保翻页时索引计算正确
            textPages.last().addLine(textLine)
        }
        return absStartX to durY + textHeight * paragraphSpacing / 10f
    }

    /**
     * 排版文字
     */
    @Suppress("DEPRECATION")
    private suspend fun setTypeText(
        book: Book,
        x: Int,
        y: Float,
        text: String,
        textPaint: TextPaint,
        textHeight: Float,
        fontMetrics: Paint.FontMetrics,
        isTitle: Boolean = false,
        emptyContent: Boolean = false,
        isVolumeTitle: Boolean = false,
        srcList: LinkedList<String>? = null
    ): Pair<Int, Float> {
        var absStartX = x
        val layout = if (ReadBookConfig.useZhLayout) {
            ZhLayout(text, textPaint, visibleWidth)
        } else {
            StaticLayout(text, textPaint, visibleWidth, Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true)
        }
        var durY = when {
            //标题y轴居中
            emptyContent && textPages.size == 1 -> {
                val textPage = textPages.last()
                if (textPage.lineSize == 0) {
                    val ty = (visibleHeight - layout.lineCount * textHeight) / 2
                    if (ty > titleTopSpacing) ty else titleTopSpacing.toFloat()
                } else {
                    var textLayoutHeight = layout.lineCount * textHeight
                    val fistLine = textPage.getLine(0)
                    if (fistLine.lineTop < textLayoutHeight + titleTopSpacing) {
                        textLayoutHeight = fistLine.lineTop - titleTopSpacing
                    }
                    textPage.lines.forEach {
                        it.lineTop -= textLayoutHeight
                        it.lineBase -= textLayoutHeight
                        it.lineBottom -= textLayoutHeight
                    }
                    y - textLayoutHeight
                }
            }

            isTitle && textPages.size == 1 && textPages.last().lines.isEmpty() ->
                y + titleTopSpacing

            else -> y
        }
        for (lineIndex in 0 until layout.lineCount) {
            val textLine = TextLine(isTitle = isTitle)
            if (durY + textHeight > visibleHeight) {
                val textPage = textPages.last()
                if (doublePage && absStartX < viewWidth / 2) {
                    //当前页面左列结束
                    textPage.leftLineSize = textPage.lineSize
                    absStartX = viewWidth / 2 + paddingLeft
                } else {
                    //当前页面结束,设置各种值
                    if (textPage.leftLineSize == 0) {
                        textPage.leftLineSize = textPage.lineSize
                    }
                    textPage.text = stringBuilder.toString()
                    onPageCompleted()
                    //新建页面
                    textPages.add(TextPage())
                    stringBuilder.clear()
                    absStartX = paddingLeft
                }
                if (textPage.height < durY) {
                    textPage.height = durY
                }
                durY = 0f
            }
            val lineStart = layout.getLineStart(lineIndex)
            val lineEnd = layout.getLineEnd(lineIndex)
            val lineText = text.substring(lineStart, lineEnd)
            val (words, widths) = measureTextSplit(lineText, textPaint)
            val desiredWidth = widths.fastSum()
            when {
                lineIndex == 0 && layout.lineCount > 1 && !isTitle -> {
                    //第一行 非标题
                    textLine.text = lineText
                    addCharsToLineFirst(
                        book, absStartX, textLine, words,
                        desiredWidth, widths, srcList
                    )
                }

                lineIndex == layout.lineCount - 1 -> {
                    //最后一行
                    textLine.text = lineText
                    //标题x轴居中
                    val startX = if (
                        isTitle &&
                        (ReadBookConfig.isMiddleTitle || emptyContent || isVolumeTitle)
                    ) {
                        (visibleWidth - desiredWidth) / 2
                    } else {
                        0f
                    }
                    addCharsToLineNatural(
                        book, absStartX, textLine, words,
                        startX, !isTitle && lineIndex == 0, widths, srcList
                    )
                }

                else -> {
                    if (
                        isTitle &&
                        (ReadBookConfig.isMiddleTitle || emptyContent || isVolumeTitle)
                    ) {
                        //标题居中
                        val startX = (visibleWidth - desiredWidth) / 2
                        addCharsToLineNatural(
                            book, absStartX, textLine, words,
                            startX, false, widths, srcList
                        )
                    } else {
                        //中间行
                        textLine.text = lineText
                        addCharsToLineMiddle(
                            book, absStartX, textLine, words,
                            desiredWidth, 0f, widths, srcList
                        )
                    }
                }
            }
            if (doublePage) {
                textLine.isLeftLine = absStartX < viewWidth / 2
            }
            calcTextLinePosition(textPages, textLine, stringBuilder.length)
            stringBuilder.append(lineText)
            textLine.upTopBottom(durY, textHeight, fontMetrics)
            val textPage = textPages.last()
            textPage.addLine(textLine)
            durY += textHeight * lineSpacingExtra
            if (textPage.height < durY) {
                textPage.height = durY
            }
        }
        durY += textHeight * paragraphSpacing / 10f
        return Pair(absStartX, durY)
    }

    private fun calcTextLinePosition(
        textPages: ArrayList<TextPage>,
        textLine: TextLine,
        sbLength: Int
    ) {
        val lastLine = textPages.last().lines.lastOrNull { it.paragraphNum > 0 }
            ?: textPages.getOrNull(textPages.lastIndex - 1)?.lines?.lastOrNull { it.paragraphNum > 0 }
        val paragraphNum = when {
            lastLine == null -> 1
            lastLine.isParagraphEnd -> lastLine.paragraphNum + 1
            else -> lastLine.paragraphNum
        }
        textLine.paragraphNum = paragraphNum
        textLine.chapterPosition =
            (textPages.getOrNull(textPages.lastIndex - 1)?.lines?.lastOrNull()?.run {
                chapterPosition + charSize + if (isParagraphEnd) 1 else 0
            } ?: 0) + sbLength
        textLine.pagePosition = sbLength
    }

    /**
     * 有缩进,两端对齐
     */
    private suspend fun addCharsToLineFirst(
        book: Book,
        absStartX: Int,
        textLine: TextLine,
        words: List<String>,
        /**自然排版长度**/
        desiredWidth: Float,
        textWidths: List<Float>,
        srcList: LinkedList<String>?
    ) {
        var x = 0f
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineNatural(
                book, absStartX, textLine, words,
                x, true, textWidths, srcList
            )
            return
        }
        val bodyIndent = ReadBookConfig.paragraphIndent
        for (i in bodyIndent.indices) {
            val x1 = x + indentCharWidth
            textLine.addColumn(
                TextColumn(
                    charData = ChapterProvider.indentChar,
                    start = absStartX + x,
                    end = absStartX + x1
                )
            )
            x = x1
            textLine.indentWidth = x
        }
        if (words.size > bodyIndent.length) {
            val text1 = words.subList(bodyIndent.length, words.size)
            val textWidths1 = textWidths.subList(bodyIndent.length, textWidths.size)
            addCharsToLineMiddle(
                book, absStartX, textLine, text1,
                desiredWidth, x, textWidths1, srcList
            )
        }
    }

    /**
     * 无缩进,两端对齐
     */
    private suspend fun addCharsToLineMiddle(
        book: Book,
        absStartX: Int,
        textLine: TextLine,
        words: List<String>,
        /**自然排版长度**/
        desiredWidth: Float,
        /**起始x坐标**/
        startX: Float,
        textWidths: List<Float>,
        srcList: LinkedList<String>?
    ) {
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineNatural(
                book, absStartX, textLine, words,
                startX, false, textWidths, srcList
            )
            return
        }
        val residualWidth = visibleWidth - desiredWidth
        val spaceSize = words.count { it == " " }
        if (spaceSize > 1) {
            val d = residualWidth / spaceSize
            var x = startX
            for (index in words.indices) {
                val char = words[index]
                val cw = textWidths[index]
                val x1 = if (char == " ") {
                    if (index != words.lastIndex) (x + cw + d) else (x + cw)
                } else {
                    (x + cw)
                }
                addCharToLine(
                    book, absStartX, textLine, char,
                    x, x1, index + 1 == words.size, srcList
                )
                x = x1
            }
        } else {
            val gapCount: Int = words.lastIndex
            val d = residualWidth / gapCount
            var x = startX
            for (index in words.indices) {
                val char = words[index]
                val cw = textWidths[index]
                val x1 = if (index != words.lastIndex) (x + cw + d) else (x + cw)
                addCharToLine(
                    book, absStartX, textLine, char,
                    x, x1, index + 1 == words.size, srcList
                )
                x = x1
            }
        }
        exceed(absStartX, textLine, words)
    }

    /**
     * 自然排列
     */
    private suspend fun addCharsToLineNatural(
        book: Book,
        absStartX: Int,
        textLine: TextLine,
        words: List<String>,
        startX: Float,
        hasIndent: Boolean,
        textWidths: List<Float>,
        srcList: LinkedList<String>?
    ) {
        val indentLength = ReadBookConfig.paragraphIndent.length
        var x = startX
        for (index in words.indices) {
            val char = words[index]
            val cw = textWidths[index]
            val x1 = x + cw
            addCharToLine(book, absStartX, textLine, char, x, x1, index + 1 == words.size, srcList)
            x = x1
            if (hasIndent && index == indentLength - 1) {
                textLine.indentWidth = x
            }
        }
        exceed(absStartX, textLine, words)
    }

    /**
     * 添加字符
     */
    private suspend fun addCharToLine(
        book: Book,
        absStartX: Int,
        textLine: TextLine,
        char: String,
        xStart: Float,
        xEnd: Float,
        isLineEnd: Boolean,
        srcList: LinkedList<String>?
    ) {
        val column = when {
            srcList != null && char == ChapterProvider.srcReplaceChar -> {
                val src = srcList.removeFirst()
                ImageProvider.cacheImage(book, src, ReadBook.bookSource)
                ImageColumn(
                    start = absStartX + xStart,
                    end = absStartX + xEnd,
                    src = src
                )
            }

            isLineEnd && char == ChapterProvider.reviewChar -> {
                ReviewColumn(
                    start = absStartX + xStart,
                    end = absStartX + xEnd,
                    count = 100
                )
            }

            else -> {
                TextColumn(
                    start = absStartX + xStart,
                    end = absStartX + xEnd,
                    charData = char
                )
            }
        }
        textLine.addColumn(column)
    }

    /**
     * 超出边界处理
     */
    private fun exceed(absStartX: Int, textLine: TextLine, words: List<String>) {
        val visibleEnd = absStartX + visibleWidth
        val endX = textLine.columns.lastOrNull()?.end ?: return
        if (endX > visibleEnd) {
            val cc = (endX - visibleEnd) / words.size
            for (i in 0..words.lastIndex) {
                textLine.getColumnReverseAt(i).let {
                    val py = cc * (words.size - i)
                    it.start -= py
                    it.end -= py
                }
            }
        }
    }

    private fun measureTextSplit(
        text: String,
        paint: TextPaint
    ): Pair<ArrayList<String>, ArrayList<Float>> {
        val length = text.length
        val widthsArray = FloatArray(length)
        paint.getTextWidths(text, widthsArray)
        val clusterCount = widthsArray.count { it > 0f }
        val widths = ArrayList<Float>(clusterCount)
        val stringList = ArrayList<String>(clusterCount)
        var i = 0
        while (i < length) {
            val clusterBaseIndex = i++
            widths.add(widthsArray[clusterBaseIndex])
            while (i < length && widthsArray[i] == 0f) {
                i++
            }
            stringList.add(text.substring(clusterBaseIndex, i))
        }
        return stringList to widths
    }

}
