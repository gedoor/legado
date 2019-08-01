package io.legado.app.ui.bookshelf

import android.os.Bundle
import android.widget.LinearLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.Book
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_bookshelf.*

class BookshelfActivity : VMBaseActivity<BookshelfViewModel>(R.layout.activity_bookshelf) {
    override val viewModel: BookshelfViewModel
        get() = getViewModel(BookshelfViewModel::class.java)

    private lateinit var bookshelfAdapter: BookshelfAdapter
    private var bookshelfLiveData: LiveData<PagedList<Book>>? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        if (viewModel.bookGroup == null) {
            viewModel.bookGroup = intent.getParcelableExtra("data")
        }
        viewModel.bookGroup?.let {
            title_bar.title = it.groupName
        }
        initRecyclerView()
        upRecyclerData()
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_bookshelf)
        rv_bookshelf.layoutManager = LinearLayoutManager(this)
        rv_bookshelf.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
        bookshelfAdapter = BookshelfAdapter()
        rv_bookshelf.adapter = bookshelfAdapter
    }

    private fun upRecyclerData() {
        viewModel.bookGroup?.let {
            bookshelfLiveData?.removeObservers(this)
            when (it.groupId) {
                -1 -> {

                    bookshelfLiveData =
                        LivePagedListBuilder(App.db.bookDao().observeAll(), 10).build()
                }
                -2 -> {
                    bookshelfLiveData =
                        LivePagedListBuilder(App.db.bookDao().observeLocal(), 10).build()
                }
                -3 -> {
                    bookshelfLiveData =
                        LivePagedListBuilder(App.db.bookDao().observeAudio(), 10).build()
                }
                else -> {
                    bookshelfLiveData =
                        LivePagedListBuilder(
                            App.db.bookDao().observeByGroup(it.groupId),
                            10
                        ).build()
                }
            }
            bookshelfLiveData?.observe(
                this,
                Observer { pageList -> bookshelfAdapter.submitList(pageList) })
        }
    }

}