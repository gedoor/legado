package io.legado.app.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.FragmentBookmarkBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private val adapter by lazy { BookmarkAdapter(requireContext(), this) }
    private var bookmarkFlowJob: Job? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            upBookmark(null)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    override fun upBookmark(searchKey: String?) {
        val book = viewModel.bookData.value ?: return
        bookmarkFlowJob?.cancel()
        bookmarkFlowJob = launch {
            when {
                searchKey.isNullOrBlank() -> appDb.bookmarkDao.flowByBook(book.name, book.author)
                else -> appDb.bookmarkDao.flowSearch(book.name, book.author, searchKey)
            }.collect {
                adapter.setItems(it)
            }
        }
    }


    override fun onClick(bookmark: Bookmark) {
        activity?.run {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("index", bookmark.chapterIndex)
                putExtra("chapterPos", bookmark.chapterPos)
            })
            finish()
        }
    }

    override fun onLongClick(bookmark: Bookmark) {
        showDialogFragment(BookmarkDialog(bookmark))
    }
}