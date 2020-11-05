package io.legado.app.ui.book.read.page.delegate

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.annotation.CallSuper
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.ui.book.read.page.ContentView
import io.legado.app.ui.book.read.page.PageView
import kotlin.math.abs

abstract class PageDelegate(protected val pageView: PageView) {

    protected val context: Context = pageView.context

    //起始点
    protected val startX: Float get() = pageView.startX
    protected val startY: Float get() = pageView.startY

    //上一个触碰点
    protected val lastX: Float get() = pageView.lastX
    protected val lastY: Float get() = pageView.lastY

    //触碰点
    protected val touchX: Float get() = pageView.touchX
    protected val touchY: Float get() = pageView.touchY

    protected val nextPage: ContentView get() = pageView.nextPage
    protected val curPage: ContentView get() = pageView.curPage
    protected val prevPage: ContentView get() = pageView.prevPage

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height

    protected val scroller: Scroller by lazy {
        Scroller(pageView.context, DecelerateInterpolator())
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
    var isStarted = false

    private var selectedOnDown = false

    init {
        curPage.resetPageOffset()
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
    }

    fun scroll() {
        if (scroller.computeScrollOffset()) {
            pageView.setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (isStarted) {
            onAnimStop()
            stopScroll()
        }
    }

    open fun onScroll() = Unit

    abstract fun abortAnim()

    abstract fun onAnimStart(animationSpeed: Int) //scroller start

    abstract fun onDraw(canvas: Canvas) //绘制

    abstract fun onAnimStop() //scroller finish

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
    abstract fun onTouch(event: MotionEvent)

    /**
     * 按下
     */
    fun onDown() {
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
