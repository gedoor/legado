package io.legado.app.ui.book.manga.rv

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.utils.findCenterViewPosition

class WebtoonRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : RecyclerView(context, attrs, defStyle) {

    private var atLastPosition = false
    private var atFirstPosition = false
    private var firstVisibleItemPosition = 0
    private var lastVisibleItemPosition = 0

    private var mLastCenterViewPosition = 0

    private var mPreScrollListener: IComicPreScroll? = null
    private var mNestedPreScrollListener: IComicPreScroll? = null

    override fun onScrolled(dx: Int, dy: Int) {
        super.onScrolled(dx, dy)
        val layoutManager = layoutManager
        lastVisibleItemPosition =
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)
        val layoutManager = layoutManager
        val visibleItemCount = layoutManager?.childCount ?: 0
        val totalItemCount = layoutManager?.itemCount ?: 0
        atLastPosition = visibleItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1
        atFirstPosition = firstVisibleItemPosition == 0
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int,
    ): Boolean {
        val position = findCenterViewPosition()
        if (position != NO_POSITION && position != mLastCenterViewPosition) {
            mLastCenterViewPosition = position
            mPreScrollListener?.onPreScrollListener(dx, dy, position)
        }
        mNestedPreScrollListener?.onPreScrollListener(dx, dy, position)
        return super.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    fun setPreScrollListener(iComicPreScroll: IComicPreScroll) {
        mPreScrollListener = iComicPreScroll
    }

    fun setNestedPreScrollListener(iComicPreScroll: IComicPreScroll) {
        mNestedPreScrollListener = iComicPreScroll
    }

    fun interface IComicPreScroll {

        fun onPreScrollListener(dx: Int, dy: Int, position: Int)
    }
}
