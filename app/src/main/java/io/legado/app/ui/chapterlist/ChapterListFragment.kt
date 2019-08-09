package io.legado.app.ui.chapterlist

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
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.getViewModelOfActivity
import kotlinx.android.synthetic.main.fragment_chapter_list.*

class ChapterListFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_chapter_list) {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)

    lateinit var adapter: ChapterListAdapter
    private var liveData: LiveData<PagedList<BookChapter>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initData()
    }

    private fun initRecyclerView() {
        adapter = ChapterListAdapter()
        recycler_view.layoutManager = LinearLayoutManager(requireContext())
        recycler_view.adapter = adapter
    }

    private fun initData() {
        liveData?.removeObservers(viewLifecycleOwner)
        liveData = LivePagedListBuilder(App.db.bookChapterDao().observeByBook(viewModel.bookUrl ?: ""), 30).build()
        liveData?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }
}