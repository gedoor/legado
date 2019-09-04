package io.legado.app.ui.explore

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.SearchBook
import io.legado.app.ui.bookinfo.BookInfoActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_explore_show.*
import org.jetbrains.anko.startActivity

class ExploreShowActivity : VMBaseActivity<ExploreShowViewModel>(R.layout.activity_explore_show),
    ExploreShowAdapter.CallBack {
    override val viewModel: ExploreShowViewModel
        get() = getViewModel(ExploreShowViewModel::class.java)

    private lateinit var adapter: ExploreShowAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("exploreName")?.let {
            title_bar.title = it
        }
        initRecyclerView()
        viewModel.booksData.observe(this, Observer { upData(it) })
        viewModel.initData(intent)
    }

    private fun initRecyclerView() {
        adapter = ExploreShowAdapter(this, this)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        recycler_view.adapter = adapter
    }

    private fun upData(books: List<SearchBook>) {
        adapter.addItems(books)
    }

    override fun showBookInfo(bookUrl: String) {
        startActivity<BookInfoActivity>(Pair("searchBookUrl", bookUrl))
    }
}