package io.legado.app.ui.main.bookshelf1

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogBookshelfConfigBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.FragmentBookshelf1Binding
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.audio.AudioPlayActivity
import io.legado.app.ui.book.arrange.ArrangeBookActivity
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.book.local.ImportBookActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.document.FilePickerParam
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.main.bookshelf.BookshelfViewModel
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment1 : VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf1),
    SearchView.OnQueryTextListener,
    BaseBooksAdapter.CallBack {

    private val binding by viewBinding(FragmentBookshelf1Binding::bind)
    override val viewModel: BookshelfViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private lateinit var booksAdapter: BaseBooksAdapter<*>
    private var groupId = AppConst.bookGroupNoneId
    private var bookGroupLiveData: LiveData<List<BookGroup>>? = null
    private var bookshelfLiveData: LiveData<List<Book>>? = null
    private var bookGroups: List<BookGroup> = emptyList()
    private var books: List<Book> = emptyList()

    private val importBookshelf = registerForActivityResult(FilePicker()) {
        it?.readText(requireContext())?.let { text ->
            viewModel.importBookshelf(text, groupId)
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initView()
        initGroupData()
        initBooksData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_update_toc -> activityViewModel.upToc(books)
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> GroupManageDialog()
                .show(childFragmentManager, "groupManageDialog")
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_add_url -> addBookByUrl()
            R.id.menu_arrange_bookshelf -> startActivity<ArrangeBookActivity> {
                putExtra("groupId", groupId)
            }
            R.id.menu_download -> startActivity<CacheActivity> {
                putExtra("groupId", groupId)
            }
            R.id.menu_export_bookshelf -> viewModel.exportBookshelf(books) {
                activity?.share(it)
            }
            R.id.menu_import_bookshelf -> importBookshelfAlert()
        }
    }

    private fun initView() {
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
        bookGroupLiveData?.removeObservers(this)
        bookGroupLiveData = appDb.bookGroupDao.liveDataShow().apply {
            observe(viewLifecycleOwner) {
                if (it.size != bookGroups.size) {
                    bookGroups = it
                    booksAdapter.notifyDataSetChanged()
                } else {

                }
            }
        }
    }

    private fun initBooksData() {
        bookshelfLiveData?.removeObservers(this)
        bookshelfLiveData = when (groupId) {
            AppConst.bookGroupAllId -> appDb.bookDao.observeAll()
            AppConst.bookGroupLocalId -> appDb.bookDao.observeLocal()
            AppConst.bookGroupAudioId -> appDb.bookDao.observeAudio()
            AppConst.bookGroupNoneId -> appDb.bookDao.observeNoGroup()
            else -> appDb.bookDao.observeByGroup(groupId)
        }.apply {
            observe(viewLifecycleOwner) { list ->
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
        startActivity<SearchActivity> {
            putExtra("key", query)
        }
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }

    @SuppressLint("InflateParams")
    private fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
            val bookshelfSort = getPrefInt(PreferKey.bookshelfSort)
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgSort.checkByIndex(bookshelfSort)
                        swShowUnread.isChecked = AppConfig.showUnread
                    }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    var changed = false
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfLayout, rgLayout.getCheckedIndex())
                        changed = true
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfSort, rgSort.getCheckedIndex())
                        changed = true
                    }
                    if (AppConfig.showUnread != swShowUnread.isChecked) {
                        AppConfig.showUnread = swShowUnread.isChecked
                        changed = true
                    }
                    if (changed) {
                        activity?.recreate()
                    }
                }
            }
            noButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun addBookByUrl() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.addBookByUrl(it)
                }
            }
            noButton()
        }.show()
    }

    fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    private fun importBookshelfAlert() {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, groupId)
                }
            }
            noButton()
            neutralButton(R.string.select_file) {
                importBookshelf.launch(
                    FilePickerParam(
                        mode = FilePicker.FILE,
                        allowExtensions = arrayOf("txt", "json")
                    )
                )
            }
        }.show()
    }

    override fun onItemClick(position: Int) {
        if (position < bookGroups.size) {
            val bookGroup = bookGroups[position]

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
    }
}