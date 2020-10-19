package io.legado.app.base.adapter

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Invincible on 2017/12/15.
 *
 * 上拉加载更多
 */
@Suppress("unused")
abstract class InfiniteScrollListener() : RecyclerView.OnScrollListener() {
    private val loadMoreRunnable = Runnable { onLoadMore() }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//        if (dy < 0 || dataLoading.isDataLoading()) return

        val layoutManager: LinearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visibleItemCount = recyclerView.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        if (totalItemCount - visibleItemCount <= firstVisibleItem + VISIBLE_THRESHOLD) {
            recyclerView.post(loadMoreRunnable)
        }
    }

    abstract fun onLoadMore()

    companion object {
        private const val VISIBLE_THRESHOLD = 5
    }
}
