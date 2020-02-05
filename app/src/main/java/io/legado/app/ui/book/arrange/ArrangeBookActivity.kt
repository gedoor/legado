package io.legado.app.ui.book.arrange

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_arrange_book.*


class ArrangeBookActivity : VMBaseActivity<ArrangeBookViewModel>(R.layout.activity_arrange_book) {
    override val viewModel: ArrangeBookViewModel
        get() = getViewModel(ArrangeBookViewModel::class.java)
    private lateinit var adapter: ArrangeBookAdapter
    private var booksLiveData: LiveData<List<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.arrange_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ArrangeBookAdapter(this)
        recycler_view.adapter = adapter
    }

    private fun initData() {
        booksLiveData?.removeObservers(this)
        booksLiveData = App.db.bookDao().observeAll()
        booksLiveData?.observe(this, Observer {

        })
    }

}