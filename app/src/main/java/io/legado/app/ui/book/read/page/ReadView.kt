package io.legado.app.ui.book.read.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.book.read.page.api.DataSource
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.ui.book.read.page.entities.PageDirection
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.ui.book.read.page.provider.TextPageFactory
import io.legado.app.utils.activity
import io.legado.app.utils.screenshot
import java.text.BreakIterator
import java.util.*
import kotlin.math.abs


class ReadView(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs),
    DataSource {

    val callBack: CallBack get() = activity as CallBack
    var pageFactory: TextPageFactory = TextPageFactory(this)
    var pageDelegate: PageDelegate? = null
        private set(value) {
            field?.onDestroy()
            field = null
            field = value
            upContent()
        }
    var isScroll = false
    var prevPage: PageView = PageView(context)
    var curPage: PageView = PageView(context)
    var nextPage: PageView = PageView(context)
    val defaultAnimationSpeed = 300
    private var pressDown = false
    private var isMove = false

    //起始点
    var startX: Float = 0f
    var startY: Float = 0f

    //上一个触碰点
    var lastX: Float = 0f
    var lastY: Float = 0f

    //触碰点
    var touchX: Float = 0f
    var touchY: Float = 0f

    //是否停止动画动作
    var isAbortAnim = false

    //长按
    private var longPressed = false
    private val longPressTimeout = 600L
    private val longPressRunnable = Runnable {
        longPressed = true
        onLongPress()
    }
    var isTextSelected = false
    private var pressOnTextSelected = false
    private var firstRelativePage = 0
    private var firstLineIndex: Int = 0
    private var firstCharIndex: Int = 0

    val slopSquare by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private val tlRect = RectF()
    private val tcRect = RectF()
    private val trRect = RectF()
    private val mlRect = RectF()
    private val mcRect = RectF()
    private val mrRect = RectF()
    private val blRect = RectF()
    private val bcRect = RectF()
    private val brRect = RectF()
    private val autoPageRect by lazy { Rect() }
    private val autoPagePint by lazy { Paint().apply { color = context.accentColor } }
    private val boundary by lazy { BreakIterator.getWordInstance(Locale.getDefault()) }

    init {
        addView(nextPage)
        addView(curPage)
        addView(prevPage)
        if (!isInEditMode) {
            upBg()
            setWillNotDraw(false)
            upPageAnim()
        }
        setRect9x()
    }

    public fun setRect9x() {
        val edge = if (AppConfig.fullScreenGesturesSupport) { 200f } else { 0f }
        tlRect.set(0f + edge, 0f, width * 0.33f, height * 0.33f)
        tcRect.set(width * 0.33f, 0f, width * 0.66f, height * 0.33f)
        trRect.set(width * 0.36f, 0f, width - 0f - edge, height * 0.33f)
        mlRect.set(0f + edge, height * 0.33f, width * 0.33f, height * 0.66f)
        mcRect.set(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
        mrRect.set(width * 0.66f, height * 0.33f, width - 0f - edge, height * 0.66f)
        blRect.set(0f + edge, height * 0.66f, width * 0.33f, height - 10f - edge)
        bcRect.set(width * 0.33f, height * 0.66f, width * 0.66f, height - 0f - edge)
        brRect.set(width * 0.66f, height * 0.66f, width - 0f - edge, height - 0f - edge)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setRect9x()
        prevPage.x = -w.toFloat()
        pageDelegate?.setViewSize(w, h)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pageDelegate?.onDraw(canvas)
        if (!isInEditMode && callBack.isAutoPage && !isScroll) {
            // TODO 自动翻页
            nextPage.screenshot()?.let {
                val bottom = callBack.autoPageProgress
                autoPageRect.set(0, 0, width, bottom)
                canvas.drawBitmap(it, autoPageRect, autoPageRect, null)
                canvas.drawRect(
                    0f,
                    bottom.toFloat() - 1,
                    width.toFloat(),
                    bottom.toFloat(),
                    autoPagePint
                )
            }
        }
    }

    override fun computeScroll() {
        pageDelegate?.scroll()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    /**
     * 触摸事件
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        callBack.screenOffTimerStart()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isTextSelected) {
                    curPage.cancelSelect()
                    isTextSelected = false
                    pressOnTextSelected = true
                } else {
                    pressOnTextSelected = false
                }
                longPressed = false
                postDelayed(longPressRunnable, longPressTimeout)
                pressDown = true
                isMove = false
                pageDelegate?.onTouch(event)
                pageDelegate?.onDown()
                setStartPoint(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isMove) {
                    isMove =
                        abs(startX - event.x) > slopSquare || abs(startY - event.y) > slopSquare
                }
                if (isMove) {
                    longPressed = false
                    removeCallbacks(longPressRunnable)
                    if (isTextSelected) {
                        selectText(event.x, event.y)
                    } else {
                        pageDelegate?.onTouch(event)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                removeCallbacks(longPressRunnable)
                if (!pressDown) return true
                pressDown = false
                if (!isMove) {
                    if (!longPressed && !pressOnTextSelected) {
                        onSingleTapUp()
                        return true
                    }
                }
                if (isTextSelected) {
                    callBack.showTextActionMenu()
                } else if (isMove) {
                    pageDelegate?.onTouch(event)
                }
                pressOnTextSelected = false
            }
        }
        return true
    }

    fun upStatusBar() {
        curPage.upStatusBar()
        prevPage.upStatusBar()
        nextPage.upStatusBar()
    }

    /**
     * 保存开始位置
     */
    fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y
        lastX = x
        lastY = y
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }
    }

    /**
     * 保存当前位置
     */
    fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        lastX = touchX
        lastY = touchY
        touchX = x
        touchY = y
        if (invalidate) {
            invalidate()
        }
        pageDelegate?.onScroll()
    }

    /**
     * 长按选择
     */
    private fun onLongPress() {
        kotlin.runCatching {
            with(curPage.textPage) {
                curPage.selectText(startX, startY) { relativePage, lineIndex, charIndex ->
                    isTextSelected = true
                    firstRelativePage = relativePage
                    firstLineIndex = lineIndex
                    firstCharIndex = charIndex
                    var lineStart = lineIndex
                    var lineEnd = lineIndex
                    var start: Int
                    var end: Int
                    if (lineIndex - 1 > 0 && lineIndex + 1 < lineSize) {
                        // 中间行
                        val lineText = with(textLines) {
                            get(lineIndex - 1).text + get(lineIndex).text + get(lineIndex + 1).text
                        }
                        boundary.setText(lineText)
                        start = boundary.first()
                        end = boundary.next()
                        val cIndex = textLines[lineIndex - 1].text.length + charIndex
                        while (end != BreakIterator.DONE) {
                            if (cIndex in start until end) {
                                break
                            }
                            start = end
                            end = boundary.next()
                        }
                        if (start < textLines[lineIndex - 1].text.length) {
                            lineStart = lineIndex - 1
                        } else {
                            start -= textLines[lineIndex - 1].text.length
                        }
                        if (end > textLines[lineIndex - 1].text.length + textLines[lineIndex].text.length) {
                            lineEnd = lineIndex + 1
                            end = (end - textLines[lineIndex - 1].text.length
                                    - textLines[lineIndex].text.length)
                        } else {
                            end = end - textLines[lineIndex - 1].text.length - 1
                        }
                    } else if (lineIndex - 1 > 0) {
                        // 尾行
                        val lineText = with(textLines) {
                            get(lineIndex - 1).text + get(lineIndex).text
                        }
                        boundary.setText(lineText)
                        start = boundary.first()
                        end = boundary.next()
                        val cIndex = textLines[lineIndex - 1].text.length + charIndex
                        while (end != BreakIterator.DONE) {
                            if (cIndex in start until end) {
                                break
                            }
                            start = end
                            end = boundary.next()
                        }
                        if (start < textLines[lineIndex - 1].text.length) {
                            lineStart = lineIndex - 1
                        } else {
                            start -= textLines[lineIndex - 1].text.length
                        }
                        end = end - textLines[lineIndex - 1].text.length - 1
                    } else if (lineIndex + 1 < lineSize) {
                        // 首行
                        val lineText = with(textLines) {
                            get(lineIndex).text + get(lineIndex + 1).text
                        }
                        boundary.setText(lineText)
                        start = boundary.first()
                        end = boundary.next()
                        while (end != BreakIterator.DONE) {
                            if (charIndex in start until end) {
                                break
                            }
                            start = end
                            end = boundary.next()
                        }
                        if (end > textLines[lineIndex].text.length) {
                            lineEnd = lineIndex + 1
                            end -= textLines[lineIndex].text.length
                        } else {
                            end -= 1
                        }
                    } else {
                        // 单行
                        val lineText = textLines[lineIndex].text
                        boundary.setText(lineText)
                        start = boundary.first()
                        end = boundary.next()
                        while (end != BreakIterator.DONE) {
                            if (charIndex in start until end) {
                                break
                            }
                            start = end
                            end = boundary.next()
                        }
                        end -= 1
                    }
                    curPage.selectStartMoveIndex(firstRelativePage, lineStart, start)
                    curPage.selectEndMoveIndex(firstRelativePage, lineEnd, end)
                }
            }
        }
    }

    /**
     * 单击
     */
    private fun onSingleTapUp() {
        when {
            isTextSelected -> isTextSelected = false
            mcRect.contains(startX, startY) -> if (!isAbortAnim) {
                click(AppConfig.clickActionMC)
            }
            bcRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBC)
            }
            blRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBL)
            }
            brRect.contains(startX, startY) -> {
                click(AppConfig.clickActionBR)
            }
            mlRect.contains(startX, startY) -> {
                click(AppConfig.clickActionML)
            }
            mrRect.contains(startX, startY) -> {
                click(AppConfig.clickActionMR)
            }
            tlRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTL)
            }
            tcRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTC)
            }
            trRect.contains(startX, startY) -> {
                click(AppConfig.clickActionTR)
            }
        }
    }

    private fun click(action: Int) {
        when (action) {
            0 -> callBack.showActionMenu()
            1 -> pageDelegate?.nextPageByAnim(defaultAnimationSpeed)
            2 -> pageDelegate?.prevPageByAnim(defaultAnimationSpeed)
            3 -> ReadBook.moveToNextChapter(true)
            4 -> ReadBook.moveToPrevChapter(upContent = true, toLast = false)
        }
    }

    /**
     * 选择文本
     */
    private fun selectText(x: Float, y: Float) {
        curPage.selectText(x, y) { relativePage, lineIndex, charIndex ->
            when {
                relativePage > firstRelativePage -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                relativePage < firstRelativePage -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
                lineIndex > firstLineIndex -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                lineIndex < firstLineIndex -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
                charIndex > firstCharIndex -> {
                    curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectEndMoveIndex(relativePage, lineIndex, charIndex)
                }
                else -> {
                    curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
                    curPage.selectStartMoveIndex(relativePage, lineIndex, charIndex)
                }
            }
        }
    }

    fun onDestroy() {
        pageDelegate?.onDestroy()
        curPage.cancelSelect()
    }

    fun fillPage(direction: PageDirection): Boolean {
        return when (direction) {
            PageDirection.PREV -> {
                pageFactory.moveToPrev(true)
            }
            PageDirection.NEXT -> {
                pageFactory.moveToNext(true)
            }
            else -> false
        }
    }

    fun upPageAnim() {
        isScroll = ReadBook.pageAnim() == 3
        when (ReadBook.pageAnim()) {
            0 -> if (pageDelegate !is CoverPageDelegate) {
                pageDelegate = CoverPageDelegate(this)
            }
            1 -> if (pageDelegate !is SlidePageDelegate) {
                pageDelegate = SlidePageDelegate(this)
            }
            2 -> if (pageDelegate !is SimulationPageDelegate) {
                pageDelegate = SimulationPageDelegate(this)
            }
            3 -> if (pageDelegate !is ScrollPageDelegate) {
                pageDelegate = ScrollPageDelegate(this)
            }
            else -> if (pageDelegate !is NoAnimPageDelegate) {
                pageDelegate = NoAnimPageDelegate(this)
            }
        }
    }

    override fun upContent(relativePosition: Int, resetPageOffset: Boolean) {
        curPage.setContentDescription(pageFactory.curPage.text)
        if (isScroll && !callBack.isAutoPage) {
            curPage.setContent(pageFactory.curPage, resetPageOffset)
        } else {
            curPage.resetPageOffset()
            when (relativePosition) {
                -1 -> prevPage.setContent(pageFactory.prevPage)
                1 -> nextPage.setContent(pageFactory.nextPage)
                else -> {
                    curPage.setContent(pageFactory.curPage)
                    nextPage.setContent(pageFactory.nextPage)
                    prevPage.setContent(pageFactory.prevPage)
                }
            }
        }
        callBack.screenOffTimerStart()
    }

    fun upStyle() {
        ChapterProvider.upStyle()
        curPage.upStyle()
        prevPage.upStyle()
        nextPage.upStyle()
    }

    fun upBg() {
        ReadBookConfig.bg ?: let {
            ReadBookConfig.upBg()
        }
        curPage.setBg(ReadBookConfig.bg)
        prevPage.setBg(ReadBookConfig.bg)
        nextPage.setBg(ReadBookConfig.bg)
    }

    fun upTime() {
        curPage.upTime()
        prevPage.upTime()
        nextPage.upTime()
    }

    fun upBattery(battery: Int) {
        curPage.upBattery(battery)
        prevPage.upBattery(battery)
        nextPage.upBattery(battery)
    }

    override val currentChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(0) else null
        }

    override val nextChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(1) else null
        }

    override val prevChapter: TextChapter?
        get() {
            return if (callBack.isInitFinish) ReadBook.textChapter(-1) else null
        }

    override fun hasNextChapter(): Boolean {
        return ReadBook.durChapterIndex < ReadBook.chapterSize - 1
    }

    override fun hasPrevChapter(): Boolean {
        return ReadBook.durChapterIndex > 0
    }

    interface CallBack {
        val isInitFinish: Boolean
        val isAutoPage: Boolean
        val autoPageProgress: Int
        fun showActionMenu()
        fun screenOffTimerStart()
        fun showTextActionMenu()
        fun autoPageStop()
    }
}
