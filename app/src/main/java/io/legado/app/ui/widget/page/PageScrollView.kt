package io.legado.app.ui.widget.page

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView


class PageScrollView : ScrollView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var scrollListener: OnScrollListener? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        scrollListener?.onScroll(t)
    }

    interface OnScrollListener {
        fun onScroll(scrollY: Int)
    }
}