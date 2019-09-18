package io.legado.app.ui.book.source.manage

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.help.storage.Restore
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.CheckSourceService
import io.legado.app.ui.book.source.edit.SourceEditActivity
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.utils.getViewModel
import io.legado.app.utils.splitNotBlank
import kotlinx.android.synthetic.main.activity_book_source.*
import kotlinx.android.synthetic.main.view_search.*
import kotlinx.android.synthetic.main.view_title_bar.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.startService

class BookSourceActivity : VMBaseActivity<BookSourceViewModel>(R.layout.activity_book_source),
    BookSourceAdapter.CallBack,
    SearchView.OnQueryTextListener {
    override val viewModel: BookSourceViewModel
        get() = getViewModel(BookSourceViewModel::class.java)

    private val qrRequestCode = 101
    private lateinit var adapter: BookSourceAdapter
    private var bookSourceLiveDate: LiveData<List<BookSource>>? = null
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setSupportActionBar(toolbar)
        initRecyclerView()
        initSearchView()
        initLiveDataBookSource()
        initLiveDataGroup()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_book_source -> {
                this.startActivity<SourceEditActivity>()
            }
            R.id.menu_import_book_source_qr -> {
                this.startActivityForResult<QrCodeActivity>(qrRequestCode)
            }
            R.id.menu_group_manage -> GroupManageDialog().show(
                supportFragmentManager,
                "groupManage"
            )
            R.id.menu_import_book_source_local -> Restore.importYueDuData(this)
            R.id.menu_select_all -> adapter.selectAll()
            R.id.menu_revert_selection -> adapter.revertSelection()
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelectionIds())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelectionIds())
            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelectionIds())
            R.id.menu_check_book_source ->
                startService<CheckSourceService>(Pair("data", adapter.getSelectionIds()))
        }
        if (item.groupId == R.id.source_group) {
            search_view.setQuery(item.title, true)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        adapter = BookSourceAdapter(this, this)
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_book_source)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(this)
    }

    private fun initLiveDataBookSource(searchKey: String? = null) {
        bookSourceLiveDate?.removeObservers(this)
        bookSourceLiveDate = if (searchKey.isNullOrEmpty()) {
            App.db.bookSourceDao().liveDataAll()
        } else {
            App.db.bookSourceDao().liveDataSearch("%$searchKey%")
        }
        bookSourceLiveDate?.observe(this, Observer {
            search_view.queryHint = getString(R.string.search_book_source_num, it.size)
            val diffResult = DiffUtil.calculateDiff(
                DiffCallBack(
                    adapter.getItems(),
                    it
                )
            )
            adapter.setItemsNoNotify(it)
            diffResult.dispatchUpdatesTo(adapter)
        })
    }

    private fun initLiveDataGroup() {
        App.db.bookSourceDao().liveGroup().observe(this, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupMenu()
        })
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.source_group)
        groups.map {
            groupMenu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            initLiveDataBookSource(it)
        }
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun del(bookSource: BookSource) {
        viewModel.del(bookSource)
    }

    override fun update(vararg bookSource: BookSource) {
        viewModel.update(*bookSource)
    }

    override fun edit(bookSource: BookSource) {
        startActivity<SourceEditActivity>(Pair("data", bookSource.bookSourceUrl))
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

    override fun toTop(bookSource: BookSource) {
        viewModel.topSource(bookSource)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> {
                if (resultCode == RESULT_OK) {
                    data?.getStringExtra("result")?.let {

                    }
                }
            }
        }
    }

    override fun finish() {
        if (search_view.query.isNullOrEmpty()) {
            super.finish()
        } else {
            search_view.setQuery("", true)
        }
    }
}