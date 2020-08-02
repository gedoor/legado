package io.legado.app.ui.book.chapterlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Bookmark
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getViewModelOfActivity
import kotlinx.android.synthetic.main.fragment_bookmark.*


class BookmarkFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    ChapterListViewModel.BookmarkCallBack {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)

    private lateinit var adapter: BookmarkAdapter
    private var bookmarkLiveData: LiveData<PagedList<Bookmark>>? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        initData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        adapter = BookmarkAdapter(this)
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.addItemDecoration(VerticalDivider(requireContext()))
        recycler_view.adapter = adapter
    }

    private fun initData() {
        viewModel.book?.let { book ->
            bookmarkLiveData?.removeObservers(viewLifecycleOwner)
            bookmarkLiveData =
                LivePagedListBuilder(
                    App.db.bookmarkDao().observeByBook(book.bookUrl, book.name, book.author), 20
                ).build()
            bookmarkLiveData?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
        }
    }

    override fun startBookmarkSearch(newText: String?) {
        if (newText.isNullOrBlank()) {
            initData()
        } else {
            bookmarkLiveData?.removeObservers(viewLifecycleOwner)
            bookmarkLiveData = LivePagedListBuilder(
                App.db.bookmarkDao().liveDataSearch(
                    viewModel.bookUrl,
                    newText
                ), 20
            ).build()
            bookmarkLiveData?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
        }
    }

    override fun open(bookmark: Bookmark) {
        val bookmarkData = Intent()
        bookmarkData.putExtra("index", bookmark.chapterIndex)
        bookmarkData.putExtra("pageIndex", bookmark.pageIndex)
        activity?.setResult(Activity.RESULT_OK, bookmarkData)
        activity?.finish()
    }

    override fun delBookmark(bookmark: Bookmark) {
        App.db.bookmarkDao().delete(bookmark)
    }
}