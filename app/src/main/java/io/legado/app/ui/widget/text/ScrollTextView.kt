package io.legado.app.ui.widget.text

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class ScrollTextView(context: Context, attrs: AttributeSet?) : AppCompatTextView(context, attrs) {
    //滑动距离的最大边界
    private var mOffsetHeight = 0

    //是否到顶或者到底的标志
    private var mBottomFlag = false

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

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            //如果是新的按下事件，则对mBottomFlag重新初始化
            mBottomFlag = mOffsetHeight <= 0
        }
        //如果已经不要这次事件，则传出取消的信号，这里的作用不大
        if (mBottomFlag) {
            event.action = MotionEvent.ACTION_CANCEL
        }
        return super.dispatchTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        //如果是需要拦截，则再拦截，这个方法会在onScrollChanged方法之后再调用一次
        if (!mBottomFlag) parent.requestDisallowInterceptTouchEvent(true)
        return result
    }

    override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert)
        if (vert == mOffsetHeight || vert == 0) {
            //这里触发父布局或祖父布局的滑动事件
            parent.requestDisallowInterceptTouchEvent(false)
            mBottomFlag = true
        }
    }
}