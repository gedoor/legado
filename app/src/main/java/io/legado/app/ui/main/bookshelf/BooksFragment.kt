package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.utils.getViewModel
import io.legado.app.utils.getViewModelOfActivity
import kotlinx.android.synthetic.main.fragment_books.*
import org.jetbrains.anko.startActivity


class BooksFragment : VMBaseFragment<BooksViewModel>(R.layout.fragment_books),
    BooksAdapter.CallBack {

    companion object {
        fun newInstance(position: Int): BooksFragment {
            return BooksFragment().apply {
                val bundle = Bundle()
                bundle.putInt("position", position)
                arguments = bundle
            }
        }
    }

    override val viewModel: BooksViewModel
        get() = getViewModel(BooksViewModel::class.java)

    private lateinit var activityViewModel: MainViewModel
    private lateinit var booksAdapter: BooksAdapter
    private var bookshelfLiveData: LiveData<PagedList<Book>>? = null
    private var groupId = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activityViewModel = getViewModelOfActivity(MainViewModel::class.java)
        arguments?.let {
            val position = it.getInt("position", 0)

        }
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_bookshelf)
        refresh_layout.setColorSchemeColors(accentColor)
        refresh_layout.setOnRefreshListener {
            refresh_layout.isRefreshing = false
            activityViewModel.upChapterList()
        }
        rv_bookshelf.layoutManager = LinearLayoutManager(context)
        rv_bookshelf.addItemDecoration(
            DividerItemDecoration(
                rv_bookshelf.context,
                LinearLayoutManager.VERTICAL
            )
        )
        booksAdapter = BooksAdapter(this)
        rv_bookshelf.adapter = booksAdapter

    }

    private fun upRecyclerData() {
        bookshelfLiveData?.removeObservers(this)
        when (groupId) {
            -1 -> {

                bookshelfLiveData =
                    LivePagedListBuilder(App.db.bookDao().observeAll(), 10).build()
            }
            -2 -> {
                bookshelfLiveData =
                    LivePagedListBuilder(App.db.bookDao().observeLocal(), 10).build()
            }
            -3 -> {
                bookshelfLiveData =
                    LivePagedListBuilder(App.db.bookDao().observeAudio(), 10).build()
            }
            else -> {
                bookshelfLiveData =
                    LivePagedListBuilder(
                        App.db.bookDao().observeByGroup(groupId),
                        10
                    ).build()
            }
        }
        bookshelfLiveData?.observe(
            this,
            Observer { pageList -> booksAdapter.submitList(pageList) })
    }

    override fun open(book: Book) {
        context?.startActivity<ReadBookActivity>(Pair("bookUrl", book.bookUrl))
    }

    override fun openBookInfo(book: Book) {
        context?.startActivity<BookInfoActivity>(Pair("bookUrl", book.bookUrl))
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return bookUrl in activityViewModel.updateList
    }

}