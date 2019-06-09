package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_bookshelf.*
import kotlinx.android.synthetic.main.view_title_bar.*

class BookshelfFragment : BaseFragment(R.layout.fragment_bookshelf) {

    private lateinit var recentReadAdapter: RecentReadAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.bookshelf, menu)
    }

    private fun initRecyclerView() {
        rv_bookshelf.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rv_read_books.layoutManager = LinearLayoutManager(context)
        recentReadAdapter = RecentReadAdapter()
        rv_read_books.adapter = recentReadAdapter
    }

}