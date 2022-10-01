package io.legado.app.ui.widget.text

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView

/**
 * 嵌套滚动 MultiAutoCompleteTextView
 */
open class NestScrollMultiAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatMultiAutoCompleteTextView(context, attrs) {

    //是否到顶或者到底的标志
    private var disallowIntercept = true

    //滑动距离的最大边界
    private var mOffsetHeight = 0

    private val gestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                disallowIntercept = true
                return super.onDown(e)
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                val y = scrollY + distanceY
                if (y < 0 || y > mOffsetHeight) {
                    disallowIntercept = false
                    //这里触发父布局或祖父布局的滑动事件
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    disallowIntercept = true
                }
                return true
            }

        })

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        initOffsetHeight()
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        initOffsetHeight()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (lineCount > maxLines) {
            gestureDetector.onTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        //如果是需要拦截，则再拦截，这个方法会在onScrollChanged方法之后再调用一次
        if (disallowIntercept && lineCount > maxLines) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        return result
    }

    private fun initOffsetHeight() {
        val mLayoutHeight: Int

        //获得内容面板
        val mLayout = layout ?: return
        //获得内容面板的高度
        mLayoutHeight = mLayout.height
        //获取上内边距
        val paddingTop: Int = totalPaddingTop
        //获取下内边距
        val paddingBottom: Int = totalPaddingBottom

        //获得控件的实际高度
        val mHeight: Int = measuredHeight

        //计算滑动距离的边界
        mOffsetHeight = mLayoutHeight + paddingTop + paddingBottom - mHeight
        if (mOffsetHeight <= 0) {
            scrollTo(0, 0)
        }
    }

}