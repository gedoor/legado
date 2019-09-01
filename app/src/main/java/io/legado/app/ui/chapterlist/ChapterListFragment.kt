package io.legado.app.ui.chapterlist

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.Bus
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.fragment_chapter_list.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ChapterListFragment : VMBaseFragment<ChapterListViewModel>(R.layout.fragment_chapter_list),
    ChapterListAdapter.Callback {
    override val viewModel: ChapterListViewModel
        get() = getViewModelOfActivity(ChapterListViewModel::class.java)

    lateinit var adapter: ChapterListAdapter
    private var durChapterIndex = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initView()
        initData()
    }

    private fun initRecyclerView() {
        adapter = ChapterListAdapter(requireContext(), this)
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
        viewModel.bookDate.observe(viewLifecycleOwner, Observer {
            loadBookFinish(it)
        })
        viewModel.bookUrl?.let { bookUrl ->
            App.db.bookChapterDao().observeByBook(bookUrl).observe(viewLifecycleOwner, Observer {
                adapter.setItems(it)
                viewModel.bookDate.value?.let { book ->
                    loadBookFinish(book)
                }
            })
        }
    }

    private fun initView() {
        iv_chapter_top.onClick { recycler_view.scrollToPosition(0) }
        iv_chapter_bottom.onClick {
            if (adapter.itemCount > 0) {
                recycler_view.scrollToPosition(adapter.itemCount - 1)
            }
        }
        tv_current_chapter_info.onClick {
            viewModel.bookDate.value?.let {
                recycler_view.scrollToPosition(it.durChapterIndex)
            }
        }
    }

    private fun loadBookFinish(book: Book) {
        durChapterIndex = book.durChapterIndex
        tv_current_chapter_info.text = book.durChapterTitle
        recycler_view.scrollToPosition(durChapterIndex)
    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

    override fun openChapter(bookChapter: BookChapter) {
        postEvent(Bus.OPEN_CHAPTER, bookChapter)
        activity?.finish()
    }

    override fun book(): Book? {
        return viewModel.bookDate.value
    }
}