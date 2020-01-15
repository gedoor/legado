package io.legado.app.ui.chapterlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Bookmark
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModelOfActivity
import kotlinx.android.synthetic.main.fragment_bookmark.*


class BookmarkFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)

    private lateinit var adapter: BookmarkAdapter
    private var bookmarkLiveData: LiveData<PagedList<Bookmark>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = BookmarkAdapter(this)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayout.VERTICAL
            )
        )
        recycler_view.adapter = adapter
    }

    private fun initData() {
        bookmarkLiveData?.removeObservers(viewLifecycleOwner)
        bookmarkLiveData = LivePagedListBuilder(App.db.bookmarkDao().observeByBook(viewModel.bookUrl ?: ""), 20).build()
        bookmarkLiveData?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }

    override fun open(bookmark: Bookmark) {
        val bookmarkData = Intent()
        bookmarkData.putExtra("index", bookmark.chapterIndex)
        bookmarkData.putExtra("pageIndex", bookmark.pageIndex)
        activity?.setResult(Activity.RESULT_OK, bookmarkData)
        activity?.finish()
    }

    override fun delBookmark(bookmark: Bookmark) {
        bookmark?.let {
            App.db.bookmarkDao().delByBookmark(it.bookUrl, it.chapterName)
        }
    }
}