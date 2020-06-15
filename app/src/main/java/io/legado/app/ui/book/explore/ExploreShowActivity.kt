package io.legado.app.ui.book.explore

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.AppConfig
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.widget.recycler.LoadMoreView
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_explore_show.*
import org.jetbrains.anko.startActivity

class ExploreShowActivity : VMBaseActivity<ExploreShowViewModel>(R.layout.activity_explore_show),
    ExploreShowAdapter.CallBack {
    override val viewModel: ExploreShowViewModel
        get() = getViewModel(ExploreShowViewModel::class.java)

    private lateinit var adapter: ExploreShowAdapter
    private lateinit var loadMoreView: LoadMoreView
    private var isLoading = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        title_bar.title = intent.getStringExtra("exploreName")
        initRecyclerView()
        viewModel.booksData.observe(this, Observer { upData(it) })
        viewModel.initData(intent)
    }

    private fun initRecyclerView() {
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        adapter = ExploreShowAdapter(this, this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(VerticalDivider(this))
        recycler_view.adapter = adapter
        loadMoreView = LoadMoreView(this)
        adapter.addFooterView(loadMoreView)
        loadMoreView.startLoad()
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            if (loadMoreView.hasMore && !isLoading) {
                viewModel.explore()
            }
        }
    }

    private fun upData(books: List<SearchBook>) {
        isLoading = false
        if (books.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (books.isEmpty()) {
            loadMoreView.noMore()
        } else if (adapter.getItems().contains(books.first()) && adapter.getItems().contains(books.last())) {
            loadMoreView.noMore()
        } else {
            adapter.addItems(books)
        }
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity>(
            Pair("name", book.name),
            Pair("author", book.author)
        )
    }
}