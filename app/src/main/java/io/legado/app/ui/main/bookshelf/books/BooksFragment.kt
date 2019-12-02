package io.legado.app.ui.main.bookshelf.books

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.BookType
import io.legado.app.constant.Bus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getViewModel
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.fragment_books.*
import org.jetbrains.anko.startActivity


class BooksFragment : VMBaseFragment<BooksViewModel>(R.layout.fragment_books),
    BooksAdapter.CallBack {

    override val viewModel: BooksViewModel
        get() = getViewModel(BooksViewModel::class.java)

    companion object {
        fun newInstance(position: Int): BooksFragment {
            return BooksFragment().apply {
                val bundle = Bundle()
                bundle.putInt("groupId", position)
                arguments = bundle
            }
        }
    }

    private lateinit var activityViewModel: MainViewModel
    private lateinit var booksAdapter: BooksAdapter
    private var bookshelfLiveData: LiveData<List<Book>>? = null
    private var groupId = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activityViewModel = getViewModelOfActivity(MainViewModel::class.java)
        arguments?.let {
            groupId = it.getInt("groupId", -1)
        }
        initRecyclerView()
        upRecyclerData()
        observeEvent<String>(Bus.UP_BOOK) {
            booksAdapter.notification(it)
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_bookshelf)
        refresh_layout.setColorSchemeColors(accentColor)
        refresh_layout.setOnRefreshListener {
            refresh_layout.isRefreshing = false
            activityViewModel.upChapterList()
        }
        val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
        rv_bookshelf.layoutManager =
            if (bookshelfLayout == 0) {
                LinearLayoutManager(context)
            } else {
                GridLayoutManager(requireContext(), bookshelfLayout)
            }
        booksAdapter =
            if (bookshelfLayout == 0) {
                BooksAdapterList(requireContext(), this)
            } else {
                BooksAdapterGrid(requireContext(), this)
            }
        rv_bookshelf.adapter = booksAdapter
    }

    private fun upRecyclerData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = when (groupId) {
            -1 -> App.db.bookDao().observeAll()
            -2 -> App.db.bookDao().observeLocal()
            -3 -> App.db.bookDao().observeAudio()
            else -> App.db.bookDao().observeByGroup(groupId)
        }
        bookshelfLiveData?.observe(this, Observer {
            val diffResult =
                DiffUtil.calculateDiff(
                    BooksDiffCallBack(
                        booksAdapter.getItems(),
                        it
                    )
                )
            booksAdapter.setItems(it, false)
            diffResult.dispatchUpdatesTo(booksAdapter)
        })
    }

    override fun open(book: Book) {
        when (book.type) {
            BookType.audio ->
                context?.startActivity<AudioPlayActivity>(Pair("bookUrl", book.bookUrl))
            else -> context?.startActivity<ReadBookActivity>(
                Pair("bookUrl", book.bookUrl),
                Pair("key", IntentDataHelp.putData(book))
            )
        }
    }

    override fun openBookInfo(book: Book) {
        context?.startActivity<BookInfoActivity>(Pair("bookUrl", book.bookUrl))
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return bookUrl in activityViewModel.updateList
    }

}