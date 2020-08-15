package io.legado.app.ui.book.read.page.delegate

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.annotation.CallSuper
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.help.AppConfig
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.page.ContentView
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

abstract class PageDelegate(protected val pageView: PageView) :
    GestureDetector.SimpleOnGestureListener() {
    private val centerRectF = RectF(
        pageView.width * 0.33f, pageView.height * 0.33f,
        pageView.width * 0.66f, pageView.height * 0.66f
    )
    protected val context: Context = pageView.context
    private val defaultAnimationSpeed = 300
    private val longPressTimeout = 1000

    //起始点
    protected var startX: Float = 0f
    protected var startY: Float = 0f
    //上一个触碰点
    protected var lastX: Float = 0f
    protected var lastY: Float = 0f
    //触碰点
    protected var touchX: Float = 0f
    protected var touchY: Float = 0f

    protected val nextPage: ContentView get() = pageView.nextPage
    protected val curPage: ContentView get() = pageView.curPage
    protected val prevPage: ContentView get() = pageView.prevPage

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height

    private val scroller: Scroller by lazy {
        Scroller(pageView.context, DecelerateInterpolator())
    }

    protected val slopSquare by lazy {
        val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        scaledTouchSlop * scaledTouchSlop
    }

    private val detector: GestureDetector by lazy {
        GestureDetector(pageView.context, this)
    }

    private val snackBar: Snackbar by lazy {
        Snackbar.make(pageView, "", Snackbar.LENGTH_SHORT)
    }

    var isMoved = false
    var noNext = true

    //移动方向
    var mDirection = Direction.NONE
    var isCancel = false
    var isRunning = false
    private var isStarted = false
    var isTextSelected = false
    private var selectedOnDown = false

    private var firstRelativePage = 0
    private var firstLineIndex: Int = 0
    private var firstCharIndex: Int = 0

    init {
        curPage.resetPageOffset()
    }

    open fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y
        lastX = x
        lastY = y
        touchX = x
        touchY = y

        if (invalidate) {
            pageView.invalidate()
        }
    }

    open fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        lastX = touchX
        lastY = touchY
        touchX = x
        touchY = y

        if (invalidate) {
            pageView.invalidate()
        }

        onScroll()
    }

    open fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int
    ) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        isRunning = true
        isStarted = true
        pageView.invalidate()
    }

    protected fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, animationSpeed: Int) {
        val duration = if (dx != 0) {
            (animationSpeed * abs(dx)) / viewWidth
        } else {
            (animationSpeed * abs(dy)) / viewHeight
        }
        scroller.startScroll(startX, startY, dx, dy, duration)
        isRunning = true
        isStarted = true
        pageView.invalidate()
    }

    protected fun stopScroll() {
        isStarted = false
        pageView.post {
            isMoved = false
            isRunning = false
            pageView.invalidate()
        }
    }

    open fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        pageView.invalidate()
        centerRectF.set(
            width * 0.33f, height * 0.33f,
            width * 0.66f, height * 0.66f
        )
    }

    fun scroll() {
        if (scroller.computeScrollOffset()) {
            setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (isStarted) {
            onAnimStop()
            stopScroll()
        }
    }

    fun abort(): Boolean {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
            return true
        }
        return false
    }

    open fun onAnimStart(animationSpeed: Int) {}//scroller start

    open fun onDraw(canvas: Canvas) {}//绘制

    open fun onAnimStop() {}//scroller finish

    open fun onScroll() {}//移动contentView， slidePage

    abstract fun nextPageByAnim(animationSpeed: Int)

    abstract fun prevPageByAnim(animationSpeed: Int)

    open fun keyTurnPage(direction: Direction) {
        if (isRunning) return
        when (direction) {
            Direction.NEXT -> nextPageByAnim(100)
            Direction.PREV -> prevPageByAnim(100)
            else -> return
        }
    }

    @CallSuper
    open fun setDirection(direction: Direction) {
        mDirection = direction
    }

    /**
     * 触摸事件处理
     */
    @CallSuper
    open fun onTouch(event: MotionEvent) {
        abort()
        //获取点击位置
        val x = event.x
        val y = event.y
        setTouchPoint(x, y, false)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setStartPoint(x, y, false)

            }
            MotionEvent.ACTION_MOVE -> {

            }
        }
        if (!detector.onTouchEvent(event)) {
            //GestureDetector.onFling小幅移动不会触发,所以要自己判断
            when (event.action) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    if (isTextSelected) {
                        pageView.callBack.showTextActionMenu()
                    }
                    if (isMoved) {
                        if (selectedOnDown) {
                            selectedOnDown = false
                        }
                        if (!noNext) onAnimStart(defaultAnimationSpeed)
                    }
                }
            }
        }
    }

    /**
     * 按下
     */
    override fun onDown(e: MotionEvent): Boolean {
        if (isTextSelected) {
            curPage.cancelSelect()
            isTextSelected = false
            selectedOnDown = true
        }
        //是否移动
        isMoved = false
        //是否存在下一章
        noNext = false
        //是否正在执行动画
        isRunning = false
        //取消
        isCancel = false
        //是下一章还是前一章
        setDirection(Direction.NONE)
        //设置起始位置的触摸点
        setStartPoint(e.x, e.y)
        return true
    }

    /**
     * 单击
     */
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        if (selectedOnDown) {
            selectedOnDown = false
            return true
        }
        if (isMoved) {
            if (!noNext) onAnimStart(defaultAnimationSpeed)
            return true
        }
        val x = e.x
        val y = e.y
        if (centerRectF.contains(x, y)) {
            pageView.callBack.clickCenter()
            setTouchPoint(x, y)
        } else if (ReadBookConfig.clickTurnPage) {
            if (x > viewWidth / 2 ||
                AppConfig.clickAllNext
            ) {
                nextPageByAnim(defaultAnimationSpeed)
            } else {
                prevPageByAnim(defaultAnimationSpeed)
            }
        }
        return true
    }

    /**
     * 长按选择
     */
    override fun onLongPress(e: MotionEvent) {
        curPage.selectText(e) { relativePage, lineIndex, charIndex ->
            isTextSelected = true
            firstRelativePage = relativePage
            firstLineIndex = lineIndex
            firstCharIndex = charIndex
        }
    }

    protected fun selectText(event: MotionEvent) {
        curPage.selectText(event) { relativePage, lineIndex, charIndex ->
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

    /**
     * 判断是否有上一页
     */
    fun hasPrev(): Boolean {
        val hasPrev = pageView.pageFactory.hasPrev()
        if (!hasPrev) {
            if (!snackBar.isShown) {
                snackBar.setText(R.string.no_prev_page)
                snackBar.show()
            }
        }
        return hasPrev
    }

    /**
     * 判断是否有下一页
     */
    fun hasNext(): Boolean {
        val hasNext = pageView.pageFactory.hasNext()
        if (!hasNext) {
            if (!snackBar.isShown) {
                snackBar.setText(R.string.no_next_page)
                snackBar.show()
            }
        }
        return hasNext
    }

    open fun onDestroy() {

    }

    enum class Direction {
        NONE, PREV, NEXT
    }

}
