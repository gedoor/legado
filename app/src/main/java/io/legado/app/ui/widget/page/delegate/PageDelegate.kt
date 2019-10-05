package io.legado.app.ui.widget.page.delegate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Scroller
import androidx.annotation.CallSuper
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import io.legado.app.ui.widget.page.ContentView
import io.legado.app.ui.widget.page.PageView
import io.legado.app.utils.screenshot
import kotlin.math.abs

abstract class PageDelegate(protected val pageView: PageView) {
    val centerRectF = RectF(
        pageView.width * 0.33f, pageView.height * 0.33f,
        pageView.width * 0.66f, pageView.height * 0.66f
    )
    //起始点
    protected var startX: Float = 0.toFloat()
    protected var startY: Float = 0.toFloat()
    //触碰点
    protected var touchX: Float = 0.toFloat()
    protected var touchY: Float = 0.toFloat()

    protected val nextPage: ContentView?
        get() = pageView.nextPage

    protected val curPage: ContentView?
        get() = pageView.curPage

    protected val prevPage: ContentView?
        get() = pageView.prevPage

    protected var bitmap: Bitmap? = null

    protected var viewWidth: Int = pageView.width
    protected var viewHeight: Int = pageView.height
    //textView在顶端或低端
    protected var atTop: Boolean = false
    protected var atBottom: Boolean = false

    private val scroller: Scroller by lazy {
        Scroller(
            pageView.context,
            FastOutLinearInInterpolator()
        )
    }

    private val detector: GestureDetector by lazy {
        GestureDetector(
            pageView.context,
            GestureListener()
        )
    }

    private var isMoved = false
    private var noNext = true

    //移动方向
    var direction = Direction.NONE
    var isCancel = false
    var isRunning = false
    var isStarted = false

    protected fun setStartPoint(x: Float, y: Float, invalidate: Boolean = true) {
        startX = x
        startY = y

        if (invalidate) {
            invalidate()
        }
    }

    protected fun setTouchPoint(x: Float, y: Float, invalidate: Boolean = true) {
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
            onScrollStop()
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
        onScrollStart()
    }

    /**
     * 触摸事件处理
     */
    @CallSuper
    open fun onTouch(event: MotionEvent): Boolean {
        if (isStarted) return false
        if (curPage?.isTextSelected() == true) {
            curPage?.dispatchTouchEvent(event)
            return true
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            curPage?.let {
                it.contentTextView()?.let { contentTextView ->
                    atTop = contentTextView.atTop()
                    atBottom = contentTextView.atBottom()
                }
                it.dispatchTouchEvent(event)
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            curPage?.dispatchTouchEvent(event)
            if (isMoved) {
                // 开启翻页效果
                if (!noNext) onScrollStart()
                return true
            }
        }
        return detector.onTouchEvent(event)
    }

    abstract fun onScrollStart()//scroller start

    abstract fun onDraw(canvas: Canvas)//绘制

    abstract fun onScrollStop()//scroller finish

    open fun onScroll() {//移动contentView， slidePage
    }

    enum class Direction {
        NONE, PREV, NEXT
    }

    /**
     * 触摸事件处理
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

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
            direction = Direction.NONE
            //设置起始位置的触摸点
            setStartPoint(e.x, e.y)
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val x = e.x
            val y = e.y
            if (centerRectF.contains(x, y)) {
                pageView.callback?.clickCenter()
                setTouchPoint(x, y)
            } else {
                bitmap = if (x > viewWidth / 2) {
                    //设置动画方向
                    if (!hasNext()) {
                        return true
                    }
                    //下一页截图
                    nextPage?.screenshot()
                } else {
                    if (!hasPrev()) {
                        return true
                    }
                    //上一页截图
                    prevPage?.screenshot()
                }
                setTouchPoint(x, y)
                onScrollStart()
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (pageView.isScrollDelegate) {
                if (!isMoved && abs(distanceX) < abs(distanceY)) {
                    if (distanceY < 0) {
                        if (atTop) {
                            val event = e1.toAction(MotionEvent.ACTION_UP)
                            curPage?.dispatchTouchEvent(event)
                            event.recycle()
                            //如果上一页不存在
                            if (!hasPrev()) {
                                noNext = true
                                return true
                            }
                            //上一页截图
                            bitmap = prevPage?.screenshot()
                        }
                    } else {
                        if (atBottom) {
                            val event = e1.toAction(MotionEvent.ACTION_UP)
                            curPage?.dispatchTouchEvent(event)
                            event.recycle()
                            //如果不存在表示没有下一页了
                            if (!hasNext()) {
                                noNext = true
                                return true
                            }
                            //下一页截图
                            bitmap = nextPage?.screenshot()
                        }
                    }
                    isMoved = true
                }
                if ((atTop && direction != Direction.PREV) || (atBottom && direction != Direction.NEXT) || direction == Direction.NONE) {
                    //传递触摸事件到textView
                    curPage?.dispatchTouchEvent(e2)
                }
            } else if (!isMoved) {
                val event = e1.toAction(MotionEvent.ACTION_UP)
                curPage?.dispatchTouchEvent(event)
                event.recycle()
                if (abs(distanceX) > abs(distanceY)) {
                    if (distanceX < 0) {
                        //如果上一页不存在
                        if (!hasPrev()) {
                            noNext = true
                            return true
                        }
                        //上一页截图
                        bitmap = prevPage?.screenshot()
                    } else {
                        //如果不存在表示没有下一页了
                        if (!hasNext()) {
                            noNext = true
                            return true
                        }
                        //下一页截图
                        bitmap = nextPage?.screenshot()
                    }
                    isMoved = true
                }
            }
            if (isMoved) {
                isCancel = if (pageView.isScrollDelegate) {
                    if (direction == Direction.NEXT) distanceY < 0 else distanceY > 0
                } else {
                    if (direction == Direction.NEXT) distanceX < 0 else distanceX > 0
                }
                isRunning = true
                //设置触摸点
                setTouchPoint(e2.x, e2.y)
            }
            return isMoved
        }
    }

    private fun hasPrev(): Boolean {
        //上一页的参数配置
        direction = Direction.PREV
        return pageView.pageFactory?.hasPrev() == true
    }

    private fun hasNext(): Boolean {
        //进行下一页的配置
        direction = Direction.NEXT
        return pageView.pageFactory?.hasNext() == true
    }
}