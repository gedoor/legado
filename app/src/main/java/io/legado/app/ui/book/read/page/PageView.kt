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
import io.legado.app.ui.book.read.page.delegate.*
import io.legado.app.ui.book.read.page.entities.TextChapter
import io.legado.app.ui.book.read.page.provider.ChapterProvider
import io.legado.app.utils.activity
import io.legado.app.utils.screenshot
import kotlinx.android.synthetic.main.activity_book_read.view.*
import kotlin.math.abs

class PageView(context: Context, attrs: AttributeSet) :
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
    var isScroll = ReadBook.pageAnim() == 3
    var prevPage: ContentView = ContentView(context)
    var curPage: ContentView = ContentView(context)
    var nextPage: ContentView = ContentView(context)
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
    private val tlRectF = RectF(10F, 10F, width * 0.33f, height * 0.33f)
    private val tcRectF = RectF(width * 0.33f, 10F, width * 0.66f, height * 0.33f)
    private val trRectF = RectF(width * 0.36f, 10F, width - 10f, height * 0.33f)
    private val mlRectF = RectF(10F, height * 0.33f, width * 0.33f, height * 0.66f)
    private val mcRectF = RectF(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
    private val mrRectF = RectF(width * 0.66f, height * 0.33f, width - 10f, height * 0.66f)
    private val blRectF = RectF(10F, height * 0.66f, width * 0.33f, height - 10f)
    private val bcRectF = RectF(width * 0.33f, height * 0.66f, width * 0.66f, height - 10f)
    private val brRectF = RectF(width * 0.66f, height * 0.66f, width - 10f, height - 10f)
    private val autoPageRect by lazy {
        Rect()
    }
    private val autoPagePint by lazy {
        Paint().apply {
            color = context.accentColor
        }
    }

    init {
        addView(nextPage)
        addView(curPage)
        addView(prevPage)
        upBg()
        setWillNotDraw(false)
        upPageAnim()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        tlRectF.set(10F, 10F, width * 0.33f, height * 0.33f)
        tcRectF.set(width * 0.33f, 10F, width * 0.66f, height * 0.33f)
        trRectF.set(width * 0.36f, 10F, width - 10f, height * 0.33f)
        mlRectF.set(10F, height * 0.33f, width * 0.33f, height * 0.66f)
        mcRectF.set(width * 0.33f, height * 0.33f, width * 0.66f, height * 0.66f)
        mrRectF.set(width * 0.66f, height * 0.33f, width - 10f, height * 0.66f)
        blRectF.set(10F, height * 0.66f, width * 0.33f, height - 10f)
        bcRectF.set(width * 0.33f, height * 0.66f, width * 0.66f, height - 10f)
        brRectF.set(width * 0.66f, height * 0.66f, width - 10f, height - 10f)
        prevPage.x = -w.toFloat()
        pageDelegate?.setViewSize(w, h)
        if (oldw != 0 && oldh != 0) {
            ReadBook.loadContent(resetPageOffset = false)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        pageDelegate?.onDraw(canvas)
        if (callBack.isAutoPage && !isScroll) {
            nextPage.screenshot()?.let {
                val bottom =
                    page_view.height * callBack.autoPageProgress / (ReadBookConfig.autoReadSpeed * 50)
                autoPageRect.set(0, 0, page_view.width, bottom)
                canvas.drawBitmap(it, autoPageRect, autoPageRect, null)
                canvas.drawRect(
                    0f,
                    bottom.toFloat() - 1,
                    page_view.width.toFloat(),
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
                pressDown = true
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
        curPage.selectText(startX, startY) { relativePage, lineIndex, charIndex ->
            isTextSelected = true
            firstRelativePage = relativePage
            firstLineIndex = lineIndex
            firstCharIndex = charIndex
            curPage.selectStartMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
            curPage.selectEndMoveIndex(firstRelativePage, firstLineIndex, firstCharIndex)
        }
    }

    /**
     * 单击
     */
    private fun onSingleTapUp(): Boolean {
        when {
            isTextSelected -> isTextSelected = false
            mcRectF.contains(startX, startY) -> if (!isAbortAnim) {
                click(AppConfig.clickActionMiddleCenter)
            }
            bcRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionBottomCenter)
            }
            blRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionBottomLeft)
            }
            brRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionBottomRight)
            }
            mlRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionMiddleLeft)
            }
            mrRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionMiddleRight)
            }
            tlRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionTopLeft)
            }
            tcRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionTopCenter)
            }
            trRectF.contains(startX, startY) -> {
                click(AppConfig.clickActionTopRight)
            }
        }
        return true
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

    fun fillPage(direction: PageDelegate.Direction) {
        when (direction) {
            PageDelegate.Direction.PREV -> {
                pageFactory.moveToPrev(true)
            }
            PageDelegate.Direction.NEXT -> {
                pageFactory.moveToNext(true)
            }
            else -> Unit
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
        if (isScroll && !callBack.isAutoPage) {
            curPage.setContent(pageFactory.curData, resetPageOffset)
        } else {
            curPage.resetPageOffset()
            when (relativePosition) {
                -1 -> prevPage.setContent(pageFactory.prevData)
                1 -> nextPage.setContent(pageFactory.nextData)
                else -> {
                    curPage.setContent(pageFactory.curData)
                    nextPage.setContent(pageFactory.nextData)
                    prevPage.setContent(pageFactory.prevData)
                }
            }
        }
        callBack.screenOffTimerStart()
    }

    fun upTipStyle() {
        curPage.upTipStyle()
        prevPage.upTipStyle()
        nextPage.upTipStyle()
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
    }
}
