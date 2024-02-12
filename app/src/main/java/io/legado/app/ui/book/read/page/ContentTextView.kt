package io.legado.app.ui.book.read.page

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.withTranslation
import io.legado.app.R
import io.legado.app.constant.PageAnim
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Bookmark
import io.legado.app.help.config.AppConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextPage
import io.legado.app.ui.book.read.page.entities.TextPos
import io.legado.app.ui.book.read.page.entities.column.BaseColumn
import io.legado.app.ui.book.read.page.entities.column.ButtonColumn
import io.legado.app.ui.book.read.page.entities.column.ImageColumn
import io.legado.app.ui.book.read.page.entities.column.ReviewColumn
import io.legado.app.ui.book.read.page.entities.column.TextColumn
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.book.read.page.provider.TextPageFactory
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.utils.PictureMirror
import io.legado.app.utils.activity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import kotlin.math.min

/**
 * 阅读内容视图
 */
class ContentTextView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var selectAble = context.getPrefBoolean(PreferKey.textSelectAble, true)
    val selectedPaint by lazy {
        Paint().apply {
            color = context.getCompatColor(R.color.btn_bg_press_2)
            style = Paint.Style.FILL
        }
    }
    private var callBack: CallBack
    private val visibleRect = RectF()
    val selectStart = TextPos(0, 0, 0)
    private val selectEnd = TextPos(0, 0, 0)
    var textPage: TextPage = TextPage()
        private set
    var isMainView = false
    private var longScreenshot = false
    var reverseStartCursor = false
    var reverseEndCursor = false

    //滚动参数
    private val pageFactory: TextPageFactory get() = callBack.pageFactory
    private var pageOffset = 0
    private val pictureMirror = PictureMirror()
    private val isNoAnim get() = ReadBook.pageAnim() == PageAnim.noAnim

    //绘制图片的paint
    val imagePaint by lazy {
        Paint().apply {
            isAntiAlias = AppConfig.useAntiAlias
        }
    }

    init {
        callBack = activity as CallBack
    }

    /**
     * 设置内容
     */
    fun setContent(textPage: TextPage) {
        this.textPage = textPage
        imagePaint.isAntiAlias = AppConfig.useAntiAlias
        invalidate()
    }

    /**
     * 更新绘制区域
     */
    fun upVisibleRect() {
        visibleRect.set(
            ChapterProvider.paddingLeft.toFloat(),
            ChapterProvider.paddingTop.toFloat(),
            ChapterProvider.visibleRight.toFloat(),
            ChapterProvider.visibleBottom.toFloat()
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!isMainView) return
        ChapterProvider.upViewSize(w, h, oldw, oldh)
        upVisibleRect()
        textPage.format()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (longScreenshot) {
            canvas.translate(0f, scrollY.toFloat())
        }
        canvas.clipRect(visibleRect)
        if (!callBack.isScroll && !isNoAnim) {
            pictureMirror.draw(canvas, width, height) {
                drawPage(this)
            }
        } else {
            drawPage(canvas)
        }
    }

    /**
     * 绘制页面
     */
    private fun drawPage(canvas: Canvas) {
        var relativeOffset = relativeOffset(0)
        val view = this
        canvas.withTranslation(0f, relativeOffset) {
            textPage.draw(view, this)
        }
        if (!callBack.isScroll) return
        //滚动翻页
        if (!pageFactory.hasNext()) return
        val textPage1 = relativePage(1)
        relativeOffset = relativeOffset(1)
        canvas.withTranslation(0f, relativeOffset) {
            textPage1.draw(view, this)
        }
        if (!pageFactory.hasNextPlus()) return
        relativeOffset = relativeOffset(2)
        if (relativeOffset < ChapterProvider.visibleHeight) {
            val textPage2 = relativePage(2)
            canvas.withTranslation(0f, relativeOffset) {
                textPage2.draw(view, this)
            }
        }
    }

    /**
     * 滚动事件
     * pageOffset 向上滚动 减小 向下滚动 增大
     * pageOffset 范围 0 ~ -textPage.height 大于0为上一页，小于-textPage.height为下一页
     * 以内容显示区域顶端为界，pageOffset的绝对值为textPage上方的高度
     * pageOffset + textPage.height 为 textPage 下方的高度
     */
    fun scroll(mOffset: Int) {
        if (mOffset == 0) return
        pageOffset += mOffset
        if (longScreenshot) {
            scrollY += -mOffset
        }
        if (!pageFactory.hasPrev() && pageOffset > 0) {
            pageOffset = 0
        } else if (!pageFactory.hasNext()
            && pageOffset < 0
            && pageOffset + textPage.height < ChapterProvider.visibleHeight
        ) {
            val offset = (ChapterProvider.visibleHeight - textPage.height).toInt()
            pageOffset = min(0, offset)
        } else if (pageOffset > 0) {
            if (pageFactory.moveToPrev(true)) {
                pageOffset -= textPage.height.toInt()
            } else {
                pageOffset = 0
            }
        } else if (pageOffset < -textPage.height) {
            val height = textPage.height
            if (pageFactory.moveToNext(upContent = true)) {
                pageOffset += height.toInt()
            } else {
                pageOffset = -height.toInt()
            }
        }
        invalidate()
    }

    override fun invalidate() {
        super.invalidate()
        pictureMirror.invalidate()
    }

    /**
     * 重置滚动位置
     */
    fun resetPageOffset() {
        pageOffset = 0
    }

    /**
     * 长按
     */
    fun longPress(
        x: Float,
        y: Float,
        select: (textPos: TextPos) -> Unit,
    ) {
        touch(x, y) { _, textPos, _, _, column ->
            when (column) {
                is ImageColumn -> callBack.onImageLongPress(x, y, column.src)
                is TextColumn -> {
                    if (!selectAble) return@touch
                    column.selected = true
                    invalidate()
                    select(textPos)
                }
            }
        }
    }

    /**
     * 单击
     * @return true:已处理, false:未处理
     */
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun click(x: Float, y: Float): Boolean {
        var handled = false
        touch(x, y) { _, textPos, textPage, textLine, column ->
            when (column) {
                is ButtonColumn -> {
                    context.toastOnUi("Button Pressed!")
                    handled = true
                }

                is ReviewColumn -> {
                    context.toastOnUi("Button Pressed!")
                    handled = true
                }

                is ImageColumn -> if (AppConfig.previewImageByClick) {
                    activity?.showDialogFragment(PhotoDialog(column.src))
                    handled = true
                }
            }
        }
        return handled
    }

    /**
     * 选择文字
     */
    fun selectText(
        x: Float,
        y: Float,
        select: (textPos: TextPos) -> Unit,
    ) {
        touchRough(x, y) { _, textPos, _, _, column ->
            if (column is TextColumn) {
                column.selected = true
                invalidate()
                select(textPos)
            }
        }
    }

    /**
     * 开始选择符移动
     */
    fun selectStartMove(x: Float, y: Float) {
        touchRough(x, y) { relativeOffset, textPos, _, textLine, textColumn ->
            if (selectStart.compare(textPos) == 0) {
                return@touchRough
            }
            if (textPos.compare(selectEnd) <= 0) {
                selectStart.upData(pos = textPos)
                upSelectedStart(
                    if (textPos.isTouch) textColumn.start else textColumn.end,
                    textLine.lineBottom + relativeOffset,
                    textLine.lineTop + relativeOffset
                )
            } else {
                reverseStartCursor = true
                reverseEndCursor = false
                selectStartMoveIndex(selectEnd)
                selectEnd.upData(textPos)
                upSelectedEnd(
                    if (selectEnd.isTouch || selectEnd.isLast) textColumn.end else textColumn.start,
                    textLine.lineBottom + relativeOffset
                )
            }
            upSelectChars()
        }
    }

    /**
     * 结束选择符移动
     */
    fun selectEndMove(x: Float, y: Float) {
        touchRough(x, y) { relativeOffset, textPos, _, textLine, textColumn ->
            if (textPos.compare(selectEnd) == 0) {
                return@touchRough
            }
            if (textPos.compare(selectStart) >= 0) {
                selectEnd.upData(textPos)
                upSelectedEnd(
                    if (selectEnd.isTouch || selectEnd.isLast) textColumn.end else textColumn.start,
                    textLine.lineBottom + relativeOffset
                )
            } else {
                reverseEndCursor = true
                reverseStartCursor = false
                selectEndMoveIndex(selectStart)
                selectStart.upData(textPos)
                upSelectedStart(
                    if (textPos.isTouch) textColumn.start else textColumn.end,
                    textLine.lineBottom + relativeOffset,
                    textLine.lineTop + relativeOffset
                )
            }
            upSelectChars()
        }
    }

    /**
     * 触碰位置信息
     * @param touched 回调
     */
    private fun touch(
        x: Float,
        y: Float,
        touched: (
            relativeOffset: Float,
            textPos: TextPos,
            textPage: TextPage,
            textLine: TextLine,
            column: BaseColumn
        ) -> Unit
    ) {
        if (!visibleRect.contains(x, y)) return
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) return
                if (relativeOffset >= ChapterProvider.visibleHeight) return
            }
            val textPage = relativePage(relativePos)
            for ((lineIndex, textLine) in textPage.lines.withIndex()) {
                if (textLine.isTouch(x, y, relativeOffset)) {
                    for ((charIndex, textColumn) in textLine.columns.withIndex()) {
                        if (textColumn.isTouch(x)) {
                            touched.invoke(
                                relativeOffset,
                                TextPos(relativePos, lineIndex, charIndex),
                                textPage, textLine, textColumn
                            )
                            return
                        }
                    }
                    return
                }
            }
        }
    }

    /**
     * 触碰位置信息
     * 文本选择专用
     * @param touched 回调
     */
    private fun touchRough(
        x: Float,
        y: Float,
        touched: (
            relativeOffset: Float,
            textPos: TextPos,
            textPage: TextPage,
            textLine: TextLine,
            column: BaseColumn
        ) -> Unit
    ) {
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) return
                if (relativeOffset >= ChapterProvider.visibleHeight) return
            }
            val textPage = relativePage(relativePos)
            for (lineIndex in textPage.lines.indices) {
                val textLine = textPage.getLine(lineIndex)
                if (textLine.isTouchY(y, relativeOffset)) {
                    if (textPage.doublePage) {
                        val halfWidth = width / 2
                        if (textLine.isLeftLine && x > halfWidth) {
                            continue
                        }
                        if (!textLine.isLeftLine && x < halfWidth) {
                            continue
                        }
                    }
                    val columns = textLine.columns
                    for (charIndex in columns.indices) {
                        val textColumn = columns[charIndex]
                        if (textColumn.isTouch(x)) {
                            touched.invoke(
                                relativeOffset,
                                TextPos(relativePos, lineIndex, charIndex),
                                textPage, textLine, textColumn
                            )
                            return
                        }
                    }
                    val isLast = columns.first().start < x
                    val charIndex = if (isLast) columns.lastIndex else 0
                    val textColumn = if (isLast) columns.last() else columns.first()
                    touched.invoke(
                        relativeOffset,
                        TextPos(relativePos, lineIndex, charIndex, false, isLast),
                        textPage, textLine, textColumn
                    )
                    return
                }
            }
        }
    }

    fun getCurVisiblePage(): TextPage {
        val visiblePage = TextPage()
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) break
                if (relativeOffset >= ChapterProvider.visibleHeight) break
            }
            val textPage = relativePage(relativePos)
            val lines = textPage.lines
            for (i in lines.indices) {
                val textLine = lines[i]
                if (textLine.isVisible(relativeOffset)) {
                    val visibleLine = textLine.copy().apply {
                        lineTop += relativeOffset
                        lineBottom += relativeOffset
                    }
                    visiblePage.addLine(visibleLine)
                }
            }
        }
        return visiblePage
    }

    fun getCurVisibleFirstLine(): TextLine? {
        var relativeOffset: Float
        for (relativePos in 0..2) {
            relativeOffset = relativeOffset(relativePos)
            if (relativePos > 0) {
                //滚动翻页
                if (!callBack.isScroll) break
                if (relativeOffset >= ChapterProvider.visibleHeight) break
            }
            val textPage = relativePage(relativePos)
            val lines = textPage.lines
            for (i in lines.indices) {
                val textLine = lines[i]
                if (textLine.isVisible(relativeOffset)) {
                    val visibleLine = textLine.copy().apply {
                        lineTop += relativeOffset
                        lineBottom += relativeOffset
                    }
                    return visibleLine
                }
            }
        }
        return null
    }

    /**
     * 选择开始文字
     */
    fun selectStartMoveIndex(
        relativePagePos: Int,
        lineIndex: Int,
        charIndex: Int,
        isTouch: Boolean,
        isLast: Boolean = false
    ) {
        selectStart.relativePagePos = relativePagePos
        selectStart.lineIndex = lineIndex
        selectStart.columnIndex = charIndex
        selectStart.isTouch = isTouch
        selectStart.isLast = isLast
        val textLine = relativePage(relativePagePos).getLine(lineIndex)
        val textColumn = textLine.getColumn(charIndex)
        upSelectedStart(
            textColumn.start,
            textLine.lineBottom + relativeOffset(relativePagePos),
            textLine.lineTop + relativeOffset(relativePagePos)
        )
        upSelectChars()
    }

    fun selectStartMoveIndex(textPos: TextPos) = textPos.run {
        selectStartMoveIndex(relativePagePos, lineIndex, columnIndex, isTouch, isLast)
    }

    /**
     * 选择结束文字
     */
    fun selectEndMoveIndex(
        relativePage: Int,
        lineIndex: Int,
        charIndex: Int,
        isTouch: Boolean,
        isLast: Boolean = false
    ) {
        selectEnd.relativePagePos = relativePage
        selectEnd.lineIndex = lineIndex
        selectEnd.columnIndex = charIndex
        selectEnd.isTouch = isTouch
        selectEnd.isLast = isLast
        val textLine = relativePage(relativePage).getLine(lineIndex)
        val textColumn = textLine.getColumn(charIndex)
        upSelectedEnd(textColumn.end, textLine.lineBottom + relativeOffset(relativePage))
        upSelectChars()
    }

    fun selectEndMoveIndex(textPos: TextPos) = textPos.run {
        selectEndMoveIndex(relativePagePos, lineIndex, columnIndex, isTouch, isLast)
    }

    private fun upSelectChars() {
        val last = if (callBack.isScroll) 2 else 0
        val textPos = TextPos(0, 0, 0)
        for (relativePos in 0..last) {
            textPos.relativePagePos = relativePos
            val textPage = relativePage(relativePos)
            for ((lineIndex, textLine) in textPage.lines.withIndex()) {
                textPos.lineIndex = lineIndex
                for ((charIndex, column) in textLine.columns.withIndex()) {
                    textPos.columnIndex = charIndex
                    if (column is TextColumn) {
                        val compareStart = textPos.compare(selectStart)
                        val compareEnd = textPos.compare(selectEnd)
                        column.selected = when {
                            compareStart == 0 -> selectStart.isTouch
                            compareEnd == 0 -> selectEnd.isTouch || selectEnd.isLast
                            compareStart > 0 && compareEnd < 0 -> true
                            else -> false
                        }
                        column.isSearchResult =
                            column.selected && callBack.isSelectingSearchResult
                        if (column.isSearchResult) {
                            textPage.searchResult.add(column)
                        }
                    }
                }
            }
        }
        invalidate()
    }

    private fun upSelectedStart(x: Float, y: Float, top: Float) {
        callBack.run {
            upSelectedStart(x, y + headerHeight, top + headerHeight)
        }
    }

    private fun upSelectedEnd(x: Float, y: Float) {
        callBack.run {
            upSelectedEnd(x, y + headerHeight)
        }
    }

    fun resetReverseCursor() {
        reverseStartCursor = false
        reverseEndCursor = false
    }

    fun cancelSelect(clearSearchResult: Boolean = false) {
        val last = if (callBack.isScroll) 2 else 0
        for (relativePos in 0..last) {
            val textPage = relativePage(relativePos)
            textPage.lines.forEach { textLine ->
                textLine.columns.forEach {
                    if (it is TextColumn) {
                        it.selected = false
                        if (clearSearchResult) {
                            it.isSearchResult = false
                            textPage.searchResult.remove(it)
                        }
                    }
                }
            }
        }
        invalidate()
        callBack.onCancelSelect()
    }

    fun getSelectedText(): String {
        val textPos = TextPos(0, 0, 0)
        val builder = StringBuilder()
        for (relativePos in selectStart.relativePagePos..selectEnd.relativePagePos) {
            val textPage = relativePage(relativePos)
            textPos.relativePagePos = relativePos
            textPage.lines.forEachIndexed { lineIndex, textLine ->
                textPos.lineIndex = lineIndex
                textLine.columns.forEachIndexed { charIndex, column ->
                    textPos.columnIndex = charIndex
                    val compareStart = textPos.compare(selectStart)
                    val compareEnd = textPos.compare(selectEnd)
                    if (column is TextColumn) {
                        when {
                            compareStart == 0 -> {
                                if (selectStart.isTouch) {
                                    builder.append(column.charData)
                                }
                                if (
                                    textLine.isParagraphEnd
                                    && charIndex == textLine.charSize - 1
                                    && compareEnd != 0
                                ) {
                                    builder.append("\n")
                                }
                            }

                            compareEnd == 0 -> if (selectEnd.isTouch || selectEnd.isLast) {
                                builder.append(column.charData)
                            }

                            compareStart > 0 && compareEnd < 0 -> {
                                builder.append(column.charData)
                                if (
                                    textLine.isParagraphEnd
                                    && charIndex == textLine.charSize - 1
                                ) {
                                    builder.append("\n")
                                }
                            }
                        }
                    }
                }
            }
        }
        return builder.toString()
    }

    fun createBookmark(): Bookmark? {
        val page = relativePage(selectStart.relativePagePos)
        page.getTextChapter()?.let { chapter ->
            ReadBook.book?.let { book ->
                return book.createBookMark().apply {
                    chapterIndex = page.chapterIndex
                    chapterPos = chapter.getReadLength(page.index) +
                            page.getPosByLineColumn(selectStart.lineIndex, selectStart.columnIndex)
                    chapterName = chapter.title
                    bookText = getSelectedText()
                }
            }
        }
        return null
    }

    private fun relativeOffset(relativePos: Int): Float {
        return when (relativePos) {
            0 -> pageOffset.toFloat()
            1 -> pageOffset + textPage.height
            else -> pageOffset + textPage.height + pageFactory.nextPage.height
        }
    }

    fun relativePage(relativePos: Int): TextPage {
        return when (relativePos) {
            0 -> textPage
            1 -> pageFactory.nextPage
            else -> pageFactory.nextPlusPage
        }
    }

    override fun canScrollVertically(direction: Int): Boolean {
        return callBack.isScroll && pageFactory.hasNext()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longScreenshot = true
                scrollY = 0
            }

            MotionEvent.ACTION_UP -> {
                longScreenshot = false
                scrollY = 0
            }
        }
        return callBack.onLongScreenshotTouchEvent(event)
    }

    interface CallBack {
        val headerHeight: Int
        val pageFactory: TextPageFactory
        val isScroll: Boolean
        var isSelectingSearchResult: Boolean
        fun upSelectedStart(x: Float, y: Float, top: Float)
        fun upSelectedEnd(x: Float, y: Float)
        fun onImageLongPress(x: Float, y: Float, src: String)
        fun onCancelSelect()
        fun onLongScreenshotTouchEvent(event: MotionEvent): Boolean
    }
}
