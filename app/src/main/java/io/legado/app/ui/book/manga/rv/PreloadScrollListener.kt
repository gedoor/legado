package io.legado.app.ui.book.manga.rv

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.legado.app.model.recyclerView.MangeContent

class PreloadScrollListener(
    private val layoutManager: LinearLayoutManager,
    private val preloadCount: Int = 3,
) : RecyclerView.OnScrollListener() {

    private val mHasSet = mutableSetOf<String>()
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val totalItemCount = layoutManager.itemCount
        if (totalItemCount == 0) return

        val lastVisible = layoutManager.findLastVisibleItemPosition()
        val preloadEnd = (lastVisible + preloadCount).coerceAtMost(totalItemCount - 1)
        for (i in lastVisible + 1..preloadEnd) {
            preloadItem(recyclerView, i)
        }
    }

    private fun preloadItem(recyclerView: RecyclerView, position: Int) {
        val adapter = recyclerView.adapter as? MangaAdapter ?: return
        if (position < 0 || position >= adapter.getCurrentList().size) return
        val item = adapter.getCurrentList()[position]
        if (item is MangeContent) {
            val url = item.mImageUrl
            if (mHasSet.add(url)) {
                Glide.with(recyclerView)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }
        }
    }
}