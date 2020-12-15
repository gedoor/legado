package io.legado.app.ui.main.bookshelf.books

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.databinding.FragmentBooksBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.main.MainViewModel
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.max

/**
 * 书架界面
 */
class BooksFragment : BaseFragment(R.layout.fragment_books),
    BaseBooksAdapter.CallBack {

    companion object {
        fun newInstance(position: Int, groupId: Long): BooksFragment {
            return BooksFragment().apply {
                val bundle = Bundle()
                bundle.putInt("position", position)
                bundle.putLong("groupId", groupId)
                arguments = bundle
            }
        }
    }

    private val binding by viewBinding(FragmentBooksBinding::bind)
    private val activityViewModel: MainViewModel
        get() = getViewModelOfActivity(MainViewModel::class.java)
    private lateinit var booksAdapter: BaseBooksAdapter<*>
    private var bookshelfLiveData: LiveData<List<Book>>? = null
    private var position = 0
    private var groupId = -1L

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let {
            position = it.getInt("position", 0)
            groupId = it.getLong("groupId", -1)
        }
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.rvBookshelf)
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(booksAdapter.getItems())
        }
        val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
        if (bookshelfLayout == 0) {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
            booksAdapter = BooksAdapterList(requireContext(), this)
        } else {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout + 2)
            booksAdapter = BooksAdapterGrid(requireContext(), this)
        }
        binding.rvBookshelf.adapter = booksAdapter
        booksAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (positionStart == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (toPosition == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }
        })
    }

    private fun upRecyclerData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = when (groupId) {
            AppConst.bookGroupAllId -> App.db.bookDao.observeAll()
            AppConst.bookGroupLocalId -> App.db.bookDao.observeLocal()
            AppConst.bookGroupAudioId -> App.db.bookDao.observeAudio()
            AppConst.bookGroupNoneId -> App.db.bookDao.observeNoGroup()
            else -> App.db.bookDao.observeByGroup(groupId)
        }.apply {
            observe(viewLifecycleOwner) { list ->
                binding.tvEmptyMsg.isGone = list.isNotEmpty()
                val books = when (getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> list.sortedBy { it.order }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
                booksAdapter.setItems(books)
            }
        }
    }

    fun getBooks(): List<Book> {
        return booksAdapter.getItems()
    }

    fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    fun getBooksCount(): Int {
        return booksAdapter.itemCount
    }

    override fun open(book: Book) {
        when (book.type) {
            BookType.audio ->
                startActivity<AudioPlayActivity>(Pair("bookUrl", book.bookUrl))
            else -> startActivity<ReadBookActivity>(
                Pair("bookUrl", book.bookUrl),
                Pair("key", IntentDataHelp.putData(book))
            )
        }
    }

    override fun openBookInfo(book: Book) {
        startActivity<BookInfoActivity>(
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