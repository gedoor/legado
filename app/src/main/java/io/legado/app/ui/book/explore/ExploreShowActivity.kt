package io.legado.app.ui.book.explore

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ActivityExploreShowBinding
import io.legado.app.databinding.DialogPageChoiceBinding
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch

class ExploreShowActivity : VMBaseActivity<ActivityExploreShowBinding, ExploreShowViewModel>(),
    ExploreShowAdapter.CallBack,
    GroupSelectDialog.CallBack {
    override val binding by viewBinding(ActivityExploreShowBinding::inflate)
    override val viewModel by viewModels<ExploreShowViewModel>()

    private val adapter by lazy { ExploreShowAdapter(this, this) }
    private val loadMoreView by lazy { LoadMoreView(this) }
    private val waitDialog by lazy {
        WaitDialog(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("exploreName")
        initRecyclerView()
        viewModel.booksData.observe(this) { upData(it) }
        viewModel.initData(intent)
        viewModel.errorLiveData.observe(this) {
            loadMoreView.error(it)
        }
        viewModel.upAdapterLiveData.observe(this) {
            adapter.notifyItemRangeChanged(0, adapter.itemCount, it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.startLoad()
        loadMoreView.setOnClickListener {
            if (!loadMoreView.isLoading) {
                loadMoreView.hasMore()
                scrollToBottom()
            }
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun scrollToBottom() {
        adapter.let {
            if (loadMoreView.hasMore && !loadMoreView.isLoading) {
                loadMoreView.startLoad()
                viewModel.explore()
            }
        }
    }

    private fun upData(books: List<SearchBook>) {
        loadMoreView.stopLoad()
        if (books.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (books.isEmpty()) {
            loadMoreView.noMore()
        } else if (adapter.getItems().contains(books.first()) && adapter.getItems()
                .contains(books.last())
        ) {
            loadMoreView.noMore()
        } else {
            adapter.addItems(books)
        }
    }

    override fun isInBookshelf(name: String, author: String): Boolean {
        return if (author.isNotBlank()) {
            viewModel.bookshelf.contains("$name-$author")
        } else {
            viewModel.bookshelf.any { it.startsWith("$name-") }
        }
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.explore_show, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_all_to_bookshelf -> addAllToBookshelf()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun addAllToBookshelf() {
        showDialogFragment(GroupSelectDialog(0))
    }

    override fun upGroup(requestCode: Int, groupId: Long) {

        alert("选择页数范围") {
            val alertBinding = DialogPageChoiceBinding.inflate(layoutInflater).apply {
                root.setBackgroundColor(root.context.backgroundColor)
            }
            customView { alertBinding.root }
            yesButton {
                alertBinding.run {
                    val start = editStart.text
                        .runCatching {
                            toString().toInt()
                        }.getOrDefault(1)
                    val end = editEnd.text
                        .runCatching {
                            toString().toInt()
                        }.getOrDefault(9)
                    addAllToBookshelf(start, end, groupId)
                }
            }
            noButton()
        }

    }

    private fun addAllToBookshelf(start: Int, end: Int, groupId: Long) {
        val job = Coroutine.async {
            launch(Main) {
                waitDialog.setText("加载列表中...")
                waitDialog.show()
            }
            val searchBooks = viewModel.loadExploreBooks(start, end)
            val books = searchBooks.map {
                it.toBook()
            }
            launch(Main) {
                waitDialog.setText("添加书架中...")
            }
            books.forEach {
                appDb.bookDao.getBook(it.bookUrl)?.let { book ->
                    book.group = book.group or groupId
                    it.order = appDb.bookDao.minOrder - 1
                    book.save()
                    return@forEach
                }
                if (it.tocUrl.isEmpty()) {
                    val source = appDb.bookSourceDao.getBookSource(it.origin)!!
                    WebBook.getBookInfoAwait(source, it)
                }
                it.order = appDb.bookDao.minOrder - 1
                it.group = groupId
                it.save()
            }
        }.onError {
            AppLog.put("添加书架出错\n${it.localizedMessage}", it)
        }.onFinally {
            waitDialog.dismiss()
        }
        waitDialog.setOnCancelListener {
            job.cancel()
        }
    }

}
