package io.legado.app.ui.main.bookshelf.style2

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.FragmentBookshelf1Binding
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.utils.cnCompare
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.observeEvent
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment2 : BaseBookshelfFragment(R.layout.fragment_bookshelf1),
    SearchView.OnQueryTextListener,
    BaseBooksAdapter.CallBack {

    private val binding by viewBinding(FragmentBookshelf1Binding::bind)
    private lateinit var searchView: SearchView
    private lateinit var booksAdapter: BaseBooksAdapter<*>
    override var groupId = AppConst.bookGroupNoneId
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private var booksFlowJob: Job? = null
    private var bookGroups: List<BookGroup> = emptyList()
    override var books: List<Book> = emptyList()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        initBooksData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
        menu.findItem(R.id.menu_search).isVisible = false
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                SearchActivity.start(requireContext(), newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.rvBookshelf)
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(books)
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

    private fun initGroupData() {
        launch {
            appDb.bookGroupDao.flowShow().collect {
                if (it != bookGroups) {
                    bookGroups = it
                    booksAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun initBooksData() {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            when (groupId) {
                AppConst.bookGroupAllId -> appDb.bookDao.flowAll()
                AppConst.bookGroupLocalId -> appDb.bookDao.flowLocal()
                AppConst.bookGroupAudioId -> appDb.bookDao.flowAudio()
                AppConst.bookGroupNoneId -> appDb.bookDao.flowNoGroup()
                else -> appDb.bookDao.flowByGroup(groupId)
            }.collect { list ->
                binding.tvEmptyMsg.isGone = list.isNotEmpty()
                books = when (getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> list.sortedByDescending {
                        it.latestChapterTime
                    }
                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> list.sortedBy {
                        it.order
                    }
                    else -> list.sortedByDescending {
                        it.durChapterTime
                    }
                }
                booksAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        SearchActivity.start(requireContext(), query)
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    override fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    override fun onItemClick(position: Int) {
        if (position < bookGroups.size) {
            val bookGroup = bookGroups[position]
            groupId = bookGroup.groupId
            initBooksData()
        } else {
            val book = books[position - bookGroups.size]
            when (book.type) {
                BookType.audio ->
                    startActivity<AudioPlayActivity> {
                        putExtra("bookUrl", book.bookUrl)
                    }
                else -> startActivity<ReadBookActivity> {
                    putExtra("bookUrl", book.bookUrl)
                }
            }
        }
    }

    override fun onItemLongClick(position: Int) {
        if (position < bookGroups.size) {
            val bookGroup = bookGroups[position]

        } else {
            val book = books[position - bookGroups.size]
            startActivity<BookInfoActivity> {
                putExtra("name", book.name)
                putExtra("author", book.author)
            }
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return bookUrl in activityViewModel.updateList
    }

    override fun getItemCount(): Int {
        return bookGroups.size + books.size
    }

    override fun getItem(position: Int): Any {
        return if (position < bookGroups.size) {
            bookGroups[position]
        } else {
            books[position - bookGroups.size]
        }
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOK) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
        }
    }
}