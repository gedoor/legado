package io.legado.app.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.DialogBookmarkBinding
import io.legado.app.databinding.FragmentBookmarkBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.requestInputMethod
import io.legado.app.utils.viewbindingdelegate.viewBinding


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel: TocViewModel by activityViewModels()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private lateinit var adapter: BookmarkAdapter
    private var bookmarkLiveData: LiveData<List<Bookmark>>? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            initData(it)
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        adapter = BookmarkAdapter(requireContext(), this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    private fun initData(book: Book) {
        bookmarkLiveData?.removeObservers(viewLifecycleOwner)
        bookmarkLiveData = appDb.bookmarkDao.observeByBook(book.name, book.author)
        bookmarkLiveData?.observe(viewLifecycleOwner, { adapter.setItems(it) })
    }

    override fun startBookmarkSearch(newText: String?) {
        viewModel.bookData.value?.let { book ->
            if (newText.isNullOrBlank()) {
                initData(book)
            } else {
                bookmarkLiveData?.removeObservers(viewLifecycleOwner)
                bookmarkLiveData = appDb.bookmarkDao.liveDataSearch(book.name, book.author, newText)
                bookmarkLiveData?.observe(viewLifecycleOwner, { adapter.setItems(it) })
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
        requireContext().alert(R.string.bookmark) {
            setMessage(bookmark.chapterName)
            val alertBinding = DialogBookmarkBinding.inflate(layoutInflater).apply {
                editBookText.setText(bookmark.bookText)
                editView.setText(bookmark.content)
                editBookText.textSize = 15f
                editView.textSize = 15f
                editBookText.maxLines= 6
                editView.maxLines= 6
            }
            customView { alertBinding.root }
            yesButton {
                alertBinding.apply {
                    Coroutine.async {
                        bookmark.bookText = editBookText.text.toString()
                        bookmark.content = editView.text.toString()
                        appDb.bookmarkDao.insert(bookmark)
                    }
                }
            }
            noButton()
            neutralButton(R.string.delete) {
                appDb.bookmarkDao.delete(bookmark)
            }
        }.show().requestInputMethod()
    }
}