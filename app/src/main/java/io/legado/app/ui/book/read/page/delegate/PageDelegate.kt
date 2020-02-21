package io.legado.app.ui.book.read.page.delegate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Scroller
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.google.android.material.snackbar.Snackbar
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.ui.book.read.page.ContentView
import io.legado.app.ui.book.read.page.PageView
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.screenshot
import kotlin.math.abs

abstract class PageDelegate(protected val pageView: PageView) :
    GestureDetector.SimpleOnGestureListener() {
    val centerRectF = RectF(
        pageView.width * 0.33f, pageView.height * 0.33f,
        pageView.width * 0.66f, pageView.height * 0.66f
    )
    protected val context: Context = pageView.context
    //起始点
    protected var startX: Float = 0.toFloat()
    protected var startY: Float = 0.toFloat()
    //触碰点
    protected var touchX: Float = 0.toFloat()
    protected var touchY: Float = 0.toFloat()

    protected val nextPage: ContentView
        get() = pageView.nextPage

    protected val curPage: ContentView
        get() = pageView.curPage

    protected val prevPage: ContentView
        get() = pageView.prevPage

    protected var bitmap: Bitmap? = null

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height
    //textView在顶端或低端
    protected var atTop: Boolean = false
    protected var atBottom: Boolean = false

    private val snackBar: Snackbar by lazy {
        Snackbar.make(pageView, "", Snackbar.LENGTH_SHORT)
    }

    private val scroller: Scroller by lazy {
        Scroller(pageView.context, FastOutLinearInInterpolator())
    }

    private val detector: GestureDetector by lazy {
        GestureDetector(pageView.context, this).apply {
            setIsLongpressEnabled(context.getPrefBoolean(PreferKey.selectText))
        }
    }

    var isMoved = false
    var noNext = true

    //移动方向
    var mDirection = Direction.NONE
    var isCancel = false
    var isRunning = false
    var isStarted = false

    open fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y

        if (invalidate) {
            invalidate()
        }
    }

    open fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
        touchX = x
        touchY = y

        if (invalidate) {
            invalidate()
        }

        onScroll()
    }

    protected fun invalidate() {
        pageView.invalidate()
    }

    protected fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
        scroller.startScroll(
            startX,
            startY,
            dx,
            dy,
            if (dx != 0) (abs(dx) * 0.3).toInt() else (abs(dy) * 0.3).toInt()
        )
        isRunning = true
        isStarted = true
        invalidate()
    }

    protected fun stopScroll() {
        isRunning = false
        isStarted = false
        invalidate()
        if (pageView.isScrollDelegate) {
            pageView.postDelayed({
                bitmap?.recycle()
                bitmap = null
            }, 100)
        } else {
            bitmap?.recycle()
            bitmap = null
        }
    }

    fun setViewSize(width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        invalidate()
        centerRectF.set(
            width * 0.33f, height * 0.33f,
            width * 0.66f, height * 0.66f
        )
    }

    fun scroll() {
        if (scroller.computeScrollOffset()) {
            setTouchPoint(scroller.currX.toFloat(), scroller.currY.toFloat())
        } else if (isStarted) {
            setTouchPoint(scroller.finalX.toFloat(), scroller.finalY.toFloat(), false)
            onAnimStop()
            stopScroll()
        }
    }

    fun abort() {
        if (!scroller.isFinished) {
            scroller.abortAnimation()
        }
    }

    fun start(direction: Direction) {
        if (isStarted) return
        if (direction === Direction.NEXT) {
            val x = viewWidth.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向
            if (!hasNext()) {
                return
            }
        } else {
            val x = 0.toFloat()
            val y = viewHeight.toFloat()
            //初始化动画
            setStartPoint(x, y, false)
            //设置点击点
            setTouchPoint(x, y, false)
            //设置方向方向
            if (!hasPrev()) {
                return
            }
        }
        onAnimStart()
    }

    abstract fun onAnimStart()//scroller start

    abstract fun onDraw(canvas: Canvas)//绘制

    abstract fun onAnimStop()//scroller finish

    open fun onScroll() {//移动contentView， slidePage
    }

    enum class Direction {
        NONE, PREV, NEXT
    }

    @CallSuper
    open fun setDirection(direction: Direction) {
        mDirection = direction
    }

    open fun setBitmap() {
        bitmap = when (mDirection) {
            Direction.NEXT -> nextPage.screenshot()
            Direction.PREV -> prevPage.screenshot()
            else -> null
        }
    }

    /**
     * 触摸事件处理
     */
    @CallSuper
    open fun onTouch(event: MotionEvent): Boolean {
        if (isStarted) return false
        curPage.dispatchTouchEvent(event)
        return detector.onTouchEvent(event)
    }

    /**
     * 按下
     */
    override fun onDown(e: MotionEvent): Boolean {
//            abort()
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
        val x = e.x
        val y = e.y
        if (centerRectF.contains(x, y)) {
            pageView.callBack?.clickCenter()
            setTouchPoint(x, y)
        } else {
            if (x > viewWidth / 2 ||
                AppConfig.clickAllNext
            ) {
                //设置动画方向
                if (!hasNext()) {
                    return true
                }
                setDirection(Direction.NEXT)
                setBitmap()
            } else {
                if (!hasPrev()) {
                    return true
                }
                setDirection(Direction.PREV)
                setBitmap()
            }
            setTouchPoint(x, y)
            onAnimStart()
        }
        return true
    }

    /**
     * 移动结束
     */
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (!noNext) onAnimStart()
        return true
    }

    /**
     * 判断是否有上一页
     */
    fun hasPrev(): Boolean {
        val hasPrev = pageView.pageFactory?.hasPrev() == true
        if (!hasPrev) {
            if (!snackBar.isShown) {
                snackBar.setText("没有上一页")
                snackBar.show()
            }
        }
        return hasPrev
    }

    /**
     * 判断是否有下一页
     */
    fun hasNext(): Boolean {
        val hasNext = pageView.pageFactory?.hasNext() == true
        if (!hasNext) {
            if (!snackBar.isShown) {
                snackBar.setText("没有下一页")
                snackBar.show()
            }
        }
        return hasNext
    }

    fun MotionEvent.toAction(action: Int): MotionEvent {
        return MotionEvent.obtain(
            downTime,
            eventTime,
            action,
            x,
            y,
            pressure,
            size,
            metaState,
            xPrecision,
            yPrecision,
            deviceId,
            edgeFlags
        )
    }
}