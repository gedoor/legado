package io.legado.app.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.SearchBook
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_explore_show.*
import kotlinx.android.synthetic.main.view_load_more.view.*
import org.jetbrains.anko.startActivity

class ExploreShowActivity : VMBaseActivity<ExploreShowViewModel>(R.layout.activity_explore_show),
    ExploreShowAdapter.CallBack {
    override val viewModel: ExploreShowViewModel
        get() = getViewModel(ExploreShowViewModel::class.java)

    private lateinit var adapter: ExploreShowAdapter
    private lateinit var loadMoreView: View
    private var hasMore = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        title_bar.title = intent.getStringExtra("exploreName")
        initRecyclerView()
        viewModel.booksData.observe(this, Observer { upData(it) })
        viewModel.initData(intent)
    }

    private fun initRecyclerView() {
        adapter = ExploreShowAdapter(this, this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        recycler_view.adapter = adapter
        loadMoreView =
            LayoutInflater.from(this).inflate(R.layout.view_load_more, recycler_view, false)
        adapter.addFooterView(loadMoreView)
        loadMoreView.rotate_loading.show()
    }

    private fun upData(books: List<SearchBook>) {
        if (books.isEmpty() && adapter.isEmpty()) {
            hasMore = false
            loadMoreView.rotate_loading.hide(View.INVISIBLE)
            loadMoreView.tv_text.text = "空"
        } else if (books.isEmpty()) {
            hasMore = false
            loadMoreView.rotate_loading.hide(View.INVISIBLE)
            loadMoreView.tv_text.text = "我是有底线的"
        } else if (adapter.getItems().contains(books.first()) && adapter.getItems().contains(books.last())) {
            hasMore = false
            loadMoreView.rotate_loading.hide(View.INVISIBLE)
            loadMoreView.tv_text.text = "我是有底线的"
        } else {
            adapter.addItems(books)
        }
    }

    override fun showBookInfo(bookUrl: String) {
        startActivity<BookInfoActivity>(Pair("searchBookUrl", bookUrl))
    }
}