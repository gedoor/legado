package io.legado.app.ui.main.findbook

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.android.flexbox.FlexboxLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.data.entities.BookSource
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import kotlinx.android.synthetic.main.fragment_find_book.*
import kotlinx.android.synthetic.main.view_title_bar.*

class FindBookFragment : BaseFragment(R.layout.fragment_find_book) {

    private lateinit var adapter: FindBookAdapter
    private var findLiveData:LiveData<PagedList<BookSource>>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
        initRecyclerView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.find_book, menu)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(rv_find)
        rv_find.layoutManager = FlexboxLayoutManager(context)
        adapter = FindBookAdapter()
        rv_find.adapter = adapter
    }

    private fun initData() {
        findLiveData?.removeObservers(viewLifecycleOwner)
        findLiveData = LivePagedListBuilder(App.db.bookSourceDao().observeFind(), 2000).build()
        findLiveData?.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
    }
}