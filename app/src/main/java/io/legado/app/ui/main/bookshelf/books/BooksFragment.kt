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
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
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
import io.legado.app.utils.getViewModelOfActivity
import io.legado.app.utils.observeEvent
import kotlinx.android.synthetic.main.fragment_books.*
import org.jetbrains.anko.startActivity


class BooksFragment : BaseFragment(R.layout.fragment_books),
    BaseBooksAdapter.CallBack {

    companion object {
        fun newInstance(position: Int, groupId: Int): BooksFragment {
            return BooksFragment().apply {
                val bundle = Bundle()
                bundle.putInt("position", position)
                bundle.putInt("groupId", groupId)
                arguments = bundle
            }
        }
    }

    private lateinit var activityViewModel: MainViewModel
    private lateinit var booksAdapter: BaseBooksAdapter
    private var bookshelfLiveData: LiveData<List<Book>>? = null
    private var position = 0
    private var groupId = -1

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        activityViewModel = getViewModelOfActivity(MainViewModel::class.java)
        arguments?.let {
            position = it.getInt("position", 0)
            groupId = it.getInt("groupId", -1)
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
        val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
        if (bookshelfLayout == 0) {
            rv_bookshelf.layoutManager = LinearLayoutManager(context)
            booksAdapter = BooksAdapterList(requireContext(), this)
        } else {
            rv_bookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout + 2)
            booksAdapter = BooksAdapterGrid(requireContext(), this)
        }
        rv_bookshelf.adapter = booksAdapter
    }

    private fun upRecyclerData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = when (groupId) {
            AppConst.bookGroupAll.groupId -> App.db.bookDao().observeAll()
            AppConst.bookGroupLocal.groupId -> App.db.bookDao().observeLocal()
            AppConst.bookGroupAudio.groupId -> App.db.bookDao().observeAudio()
            AppConst.bookGroupNone.groupId -> App.db.bookDao().observeNoGroup()
            else -> App.db.bookDao().observeByGroup(groupId)
        }
        bookshelfLiveData?.observe(this, Observer { list ->
            val books = when (getPrefInt(PreferKey.bookshelfSort)) {
                1 -> list.sortedByDescending { it.latestChapterTime }
                2 -> list.sortedBy { it.name }
                3 -> list.sortedBy { it.order }
                else -> list.sortedByDescending { it.durChapterTime }
            }
            val diffResult = DiffUtil
                .calculateDiff(BooksDiffCallBack(booksAdapter.getItems(), books))
            booksAdapter.setItems(books, diffResult)
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
        context?.startActivity<BookInfoActivity>(
            Pair("name", book.name),
            Pair("author", book.author)
        )
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return bookUrl in activityViewModel.updateList
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOK) {
            booksAdapter.notification(it)
        }
    }
}