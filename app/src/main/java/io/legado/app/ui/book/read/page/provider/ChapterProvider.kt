package io.legado.app.ui.book.read.page.provider

import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.entities.TextChar
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.utils.*
import splitties.init.appCtx
import java.util.*

/**
 * 解析内容生成章节和页面
 */
@Suppress("DEPRECATION")
object ChapterProvider {
    private const val srcReplaceChar = "▩"

    @JvmStatic
    var viewWidth = 0
        private set

    @JvmStatic
    var viewHeight = 0
        private set

    @JvmStatic
    var paddingLeft = 0
        private set

    @JvmStatic
    var paddingTop = 0
        private set

    @JvmStatic
    var paddingRight = 0
        private set

    @JvmStatic
    var paddingBottom = 0
        private set

    @JvmStatic
    var visibleWidth = 0
        private set

    @JvmStatic
    var visibleHeight = 0
        private set

    @JvmStatic
    var visibleRight = 0
        private set

    @JvmStatic
    var visibleBottom = 0
        private set

    @JvmStatic
    var lineSpacingExtra = 0f
        private set

    @JvmStatic
    private var paragraphSpacing = 0

    @JvmStatic
    private var titleTopSpacing = 0

    @JvmStatic
    private var titleBottomSpacing = 0

    @JvmStatic
    var typeface: Typeface = Typeface.DEFAULT
        private set

    @JvmStatic
    var titlePaint: TextPaint = TextPaint()

    @JvmStatic
    var contentPaint: TextPaint = TextPaint()

    var doublePage = false
        private set

    init {
        upStyle()
    }

    /**
     * 获取拆分完的章节数据
     */
    fun getTextChapter(
        book: Book,
        bookChapter: BookChapter,
        displayTitle: String,
        contents: List<String>,
        chapterSize: Int,
    ): TextChapter {
        val textPages = arrayListOf<TextPage>()
        val stringBuilder = StringBuilder()
        var absStartX = paddingLeft
        var durY = 0f
        textPages.add(TextPage())
        contents.forEachIndexed { index, content ->
            if (book.getImageStyle() == Book.imgStyleText) {
                var text = content.replace(srcReplaceChar, "▣")
                val srcList = LinkedList<String>()
                val sb = StringBuffer()
                val matcher = AppPattern.imgPattern.matcher(text)
                while (matcher.find()) {
                    matcher.group(1)?.let { src ->
                        srcList.add(src)
                        ImageProvider.getImage(book, bookChapter.index, src, ReadBook.bookSource)
                        matcher.appendReplacement(sb, srcReplaceChar)
                    }
                }
                matcher.appendTail(sb)
                text = sb.toString()
                val isTitle = index == 0
                val isVolumeTitle = bookChapter.isVolume && isTitle && contents.size == 1
                val textPaint = if (isTitle) titlePaint else contentPaint
                if (!(isTitle && ReadBookConfig.titleMode == 2)) {
                    setTypeText(
                        absStartX, durY, text, textPages, stringBuilder,
                        isTitle, isVolumeTitle, textPaint, srcList
                    ).let {
                        absStartX = it.first
                        durY = it.second
                    }
                }
            } else if (book.getImageStyle() != Book.imgStyleText) {
                val matcher = AppPattern.imgPattern.matcher(content)
                var start = 0
                while (matcher.find()) {
                    val text = content.substring(start, matcher.start())
                    if (text.isNotBlank()) {
                        val isTitle = index == 0
                        val isVolumeTitle = bookChapter.isVolume && isTitle && contents.size == 1
                        val textPaint = if (isTitle) titlePaint else contentPaint
                        if (!(isTitle && ReadBookConfig.titleMode == 2)) {
                            setTypeText(
                                absStartX, durY, text, textPages,
                                stringBuilder, isTitle, isVolumeTitle, textPaint
                            ).let {
                                absStartX = it.first
                                durY = it.second
                            }
                        }
                    }
                    durY = setTypeImage(
                        book, bookChapter, matcher.group(1)!!,
                        durY, textPages, book.getImageStyle()
                    )
                    start = matcher.end()
                }
                if (start < content.length) {
                    val text = content.substring(start, content.length)
                    if (text.isNotBlank()) {
                        val isTitle = index == 0
                        val isVolumeTitle = bookChapter.isVolume && isTitle && contents.size == 1
                        val textPaint = if (isTitle) titlePaint else contentPaint
                        if (!(isTitle && ReadBookConfig.titleMode == 2)) {
                            setTypeText(
                                absStartX, durY, text, textPages,
                                stringBuilder, isTitle, isVolumeTitle, textPaint
                            ).let {
                                absStartX = it.first
                                durY = it.second
                            }
                        }
                    }
                }
            }
        }
        textPages.last().height = durY + 20.dp
        textPages.last().text = stringBuilder.toString()
        textPages.forEachIndexed { index, item ->
            item.index = index
            item.pageSize = textPages.size
            item.chapterIndex = bookChapter.index
            item.chapterSize = chapterSize
            item.title = displayTitle
            item.upLinesPosition()
        }

        return TextChapter(
            bookChapter.index, displayTitle,
            bookChapter.getAbsoluteURL(),
            textPages, chapterSize,
            bookChapter.isVip, bookChapter.isPay
        )
    }

    private fun setTypeImage(
        book: Book,
        chapter: BookChapter,
        src: String,
        y: Float,
        textPages: ArrayList<TextPage>,
        imageStyle: String?,
    ): Float {
        var durY = y
        ImageProvider.getImage(book, chapter.index, src, ReadBook.bookSource)?.let {
            if (durY > visibleHeight) {
                textPages.last().height = durY
                textPages.add(TextPage())
                durY = 0f
            }
            var height = it.height
            var width = it.width
            when (imageStyle?.toUpperCase(Locale.ROOT)) {
                Book.imgStyleFull -> {
                    width = visibleWidth
                    height = it.height * visibleWidth / it.width
                }
                else -> {
                    if (it.width > visibleWidth) {
                        height = it.height * visibleWidth / it.width
                        width = visibleWidth
                    }
                    if (height > visibleHeight) {
                        width = width * visibleHeight / height
                        height = visibleHeight
                    }
                    if (durY + height > visibleHeight) {
                        textPages.last().height = durY
                        textPages.add(TextPage())
                        durY = 0f
                    }
                }
            }
            val textLine = TextLine(isImage = true)
            textLine.lineTop = durY
            durY += height
            textLine.lineBottom = durY
            val (start, end) = if (visibleWidth > width) {
                val adjustWidth = (visibleWidth - width) / 2f
                Pair(
                    paddingLeft.toFloat() + adjustWidth,
                    paddingLeft.toFloat() + adjustWidth + width
                )
            } else {
                Pair(paddingLeft.toFloat(), (paddingLeft + width).toFloat())
            }
            textLine.textChars.add(
                TextChar(
                    charData = src,
                    start = start,
                    end = end,
                    isImage = true
                )
            )
            textPages.last().textLines.add(textLine)
        }
        return durY + paragraphSpacing / 10f
    }

    /**
     * 排版文字
     */
    private fun setTypeText(
        x: Int,
        y: Float,
        text: String,
        textPages: ArrayList<TextPage>,
        stringBuilder: StringBuilder,
        isTitle: Boolean,
        isVolumeTitle: Boolean,
        textPaint: TextPaint,
        srcList: LinkedList<String>? = null
    ): Pair<Int, Float> {
        var absStartX = x
        val layout = if (ReadBookConfig.useZhLayout) {
            ZhLayout(text, textPaint, visibleWidth)
        } else StaticLayout(
            text, textPaint, visibleWidth, Layout.Alignment.ALIGN_NORMAL, 0f, 0f, true
        )
        var durY = when {
            isVolumeTitle -> ((visibleHeight - layout.lineCount * textPaint.textHeight) / 2).toFloat()
            isTitle -> y + titleTopSpacing
            else -> y
        }
        for (lineIndex in 0 until layout.lineCount) {
            val textLine = TextLine(isTitle = isTitle)
            if (durY + textPaint.textHeight > visibleHeight) {
                val textPage = textPages.last()
                if (doublePage && absStartX < viewWidth / 2) {
                    textPage.leftLineSize = textPage.lineSize
                    absStartX = viewWidth / 2 + paddingLeft
                } else {
                    //当前页面结束,设置各种值
                    if (textPage.leftLineSize == 0) {
                        textPage.leftLineSize = textPage.lineSize
                    }
                    textPage.text = stringBuilder.toString()
                    textPage.height = durY
                    //新建页面
                    textPages.add(TextPage())
                    stringBuilder.clear()
                    absStartX = paddingLeft
                }
                durY = 0f
            }
            val words =
                text.substring(layout.getLineStart(lineIndex), layout.getLineEnd(lineIndex))
            val desiredWidth = layout.getLineWidth(lineIndex)
            var isLastLine = false
            when {
                lineIndex == 0 && layout.lineCount > 1 && !isTitle -> {
                    //第一行 非标题
                    textLine.text = words
                    addCharsToLineFirst(
                        absStartX,
                        textLine,
                        words.toStringArray(),
                        textPaint,
                        desiredWidth,
                        srcList
                    )
                }
                lineIndex == layout.lineCount - 1 -> {
                    //最后一行
                    textLine.text = "$words\n"
                    isLastLine = true
                    //标题居中
                    val startX = if (isTitle && ReadBookConfig.titleMode == 1 || isVolumeTitle)
                        (visibleWidth - layout.getLineWidth(lineIndex)) / 2
                    else 0f
                    addCharsToLineLast(
                        absStartX,
                        textLine,
                        words.toStringArray(),
                        textPaint,
                        startX,
                        srcList
                    )
                }
                else -> {
                    //中间行
                    textLine.text = words
                    addCharsToLineMiddle(
                        absStartX,
                        textLine,
                        words.toStringArray(),
                        textPaint,
                        desiredWidth,
                        0f,
                        srcList
                    )
                }
            }
            stringBuilder.append(words)
            if (isLastLine) stringBuilder.append("\n")
            textPages.last().textLines.add(textLine)
            textLine.upTopBottom(durY, textPaint)
            durY += textPaint.textHeight * lineSpacingExtra
            textPages.last().height = durY
        }
        if (isTitle && !isVolumeTitle) durY += titleBottomSpacing
        durY += textPaint.textHeight * paragraphSpacing / 10f
        return Pair(absStartX, durY)
    }

    /**
     * 有缩进,两端对齐
     */
    private fun addCharsToLineFirst(
        absStartX: Int,
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        desiredWidth: Float,
        srcList: LinkedList<String>?
    ) {
        var x = 0f
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineLast(absStartX, textLine, words, textPaint, x, srcList)
            return
        }
        val bodyIndent = ReadBookConfig.paragraphIndent
        val icw = StaticLayout.getDesiredWidth(bodyIndent, textPaint) / bodyIndent.length
        bodyIndent.toStringArray().forEach { char ->
            val x1 = x + icw
            textLine.textChars.add(
                TextChar(
                    charData = char,
                    start = absStartX + x,
                    end = absStartX + x1
                )
            )
            x = x1
        }
        if (words.size > bodyIndent.length) {
            val words1 = words.copyOfRange(bodyIndent.length, words.size)
            addCharsToLineMiddle(absStartX, textLine, words1, textPaint, desiredWidth, x, srcList)
        }
    }

    /**
     * 无缩进,两端对齐
     */
    private fun addCharsToLineMiddle(
        absStartX: Int,
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        desiredWidth: Float,
        startX: Float,
        srcList: LinkedList<String>?
    ) {
        if (!ReadBookConfig.textFullJustify) {
            addCharsToLineLast(absStartX, textLine, words, textPaint, startX, srcList)
            return
        }
        val gapCount: Int = words.lastIndex
        val d = (visibleWidth - desiredWidth) / gapCount
        var x = startX
        words.forEachIndexed { index, char ->
            val cw = StaticLayout.getDesiredWidth(char, textPaint)
            val x1 = if (index != words.lastIndex) (x + cw + d) else (x + cw)
            if (srcList != null && char == srcReplaceChar) {
                textLine.textChars.add(
                    TextChar(
                        charData = srcList.removeFirst(),
                        start = absStartX + x,
                        end = absStartX + x1,
                        isImage = true
                    )
                )
            } else {
                textLine.textChars.add(
                    TextChar(
                        charData = char,
                        start = absStartX + x,
                        end = absStartX + x1
                    )
                )
            }
            x = x1
        }
        exceed(absStartX, textLine, words)
    }

    /**
     * 最后一行,自然排列
     */
    private fun addCharsToLineLast(
        absStartX: Int,
        textLine: TextLine,
        words: Array<String>,
        textPaint: TextPaint,
        startX: Float,
        srcList: LinkedList<String>?
    ) {
        var x = startX
        words.forEach { char ->
            val cw = StaticLayout.getDesiredWidth(char, textPaint)
            val x1 = x + cw
            if (srcList != null && char == srcReplaceChar) {
                textLine.textChars.add(
                    TextChar(
                        charData = srcList.removeFirst(),
                        start = absStartX + x,
                        end = absStartX + x1,
                        isImage = true
                    )
                )
            } else {
                textLine.textChars.add(
                    TextChar(
                        charData = char,
                        start = absStartX + x,
                        end = absStartX + x1
                    )
                )
            }
            x = x1
        }
        exceed(absStartX, textLine, words)
    }

    /**
     * 超出边界处理
     */
    private fun exceed(absStartX: Int, textLine: TextLine, words: Array<String>) {
        val visibleEnd = absStartX + visibleWidth
        val endX = textLine.textChars.lastOrNull()?.end ?: return
        if (endX > visibleEnd) {
            val cc = (endX - visibleEnd) / words.size
            for (i in 0..words.lastIndex) {
                textLine.getTextCharReverseAt(i).let {
                    val py = cc * (words.size - i)
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
        typeface = getTypeface(ReadBookConfig.textFont)
        getPaints(typeface).let {
            titlePaint = it.first
            contentPaint = it.second
        }
        //间距
        lineSpacingExtra = ReadBookConfig.lineSpacingExtra / 10f
        paragraphSpacing = ReadBookConfig.paragraphSpacing
        titleTopSpacing = ReadBookConfig.titleTopSpacing.dp
        titleBottomSpacing = ReadBookConfig.titleBottomSpacing.dp
        upLayout()
    }

    private fun getTypeface(fontPath: String): Typeface {
        return kotlin.runCatching {
            when {
                fontPath.isContentScheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val fd = appCtx.contentResolver
                        .openFileDescriptor(Uri.parse(fontPath), "r")!!
                        .fileDescriptor
                    Typeface.Builder(fd).build()
                }
                fontPath.isContentScheme() -> {
                    Typeface.createFromFile(RealPathUtil.getPath(appCtx, Uri.parse(fontPath)))
                }
                fontPath.isNotEmpty() -> Typeface.createFromFile(fontPath)
                else -> when (AppConfig.systemTypefaces) {
                    1 -> Typeface.SERIF
                    2 -> Typeface.MONOSPACE
                    else -> Typeface.SANS_SERIF
                }
            }
        }.getOrElse {
            ReadBookConfig.textFont = ""
            ReadBookConfig.save()
            Typeface.SANS_SERIF
        } ?: Typeface.DEFAULT
    }

    private fun getPaints(typeface: Typeface): Pair<TextPaint, TextPaint> {
        // 字体统一处理
        val bold = Typeface.create(typeface, Typeface.BOLD)
        val normal = Typeface.create(typeface, Typeface.NORMAL)
        val (titleFont, textFont) = when (ReadBookConfig.textBold) {
            1 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    Pair(Typeface.create(typeface, 900, false), bold)
                else
                    Pair(bold, bold)
            }
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    Pair(normal, Typeface.create(typeface, 300, false))
                else
                    Pair(normal, normal)
            }
            else -> Pair(bold, normal)
        }

        //标题
        val tPaint = TextPaint()
        tPaint.color = ReadBookConfig.textColor
        tPaint.letterSpacing = ReadBookConfig.letterSpacing
        tPaint.typeface = titleFont
        tPaint.textSize = with(ReadBookConfig) { textSize + titleSize }.sp.toFloat()
        tPaint.isAntiAlias = true
        //正文
        val cPaint = TextPaint()
        cPaint.color = ReadBookConfig.textColor
        cPaint.letterSpacing = ReadBookConfig.letterSpacing
        cPaint.typeface = textFont
        cPaint.textSize = ReadBookConfig.textSize.sp.toFloat()
        cPaint.isAntiAlias = true
        return Pair(tPaint, cPaint)
    }

    /**
     * 更新View尺寸
     */
    fun upViewSize(width: Int, height: Int) {
        if (width > 0 && height > 0 && (width != viewWidth || height != viewHeight)) {
            viewWidth = width
            viewHeight = height
            upLayout()
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    /**
     * 更新绘制尺寸
     */
    fun upLayout() {
        doublePage = (viewWidth > viewHeight || appCtx.isPad)
                && ReadBook.pageAnim() != 3
                && AppConfig.doublePageHorizontal
        if (viewWidth > 0 && viewHeight > 0) {
            paddingLeft = ReadBookConfig.paddingLeft.dp
            paddingTop = ReadBookConfig.paddingTop.dp
            paddingRight = ReadBookConfig.paddingRight.dp
            paddingBottom = ReadBookConfig.paddingBottom.dp
            visibleWidth = if (doublePage) {
                viewWidth / 2 - paddingLeft - paddingRight
            } else {
                viewWidth - paddingLeft - paddingRight
            }
            visibleHeight = viewHeight - paddingTop - paddingBottom
            visibleRight = viewWidth - paddingRight
            visibleBottom = paddingTop + visibleHeight
        }
    }

}
