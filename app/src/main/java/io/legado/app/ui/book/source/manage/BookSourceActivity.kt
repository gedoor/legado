package io.legado.app.ui.book.source.manage

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.BookSource
import io.legado.app.help.AppConfig
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.help.CheckSource
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_book_source.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.view_search.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.File
import java.text.Collator

class BookSourceActivity : VMBaseActivity<BookSourceViewModel>(R.layout.activity_book_source),
    PopupMenu.OnMenuItemClickListener,
    BookSourceAdapter.CallBack,
    FileChooserDialog.CallBack,
    SearchView.OnQueryTextListener {
    override val viewModel: BookSourceViewModel
        get() = getViewModel(BookSourceViewModel::class.java)
    private val importRecordKey = "bookSourceRecordKey"
    private val qrRequestCode = 101
    private val importRequestCode = 132
    private val exportRequestCode = 65
    private lateinit var adapter: BookSourceAdapter
    private var bookSourceLiveDate: LiveData<List<BookSource>>? = null
    private var groups = linkedSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var sort = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initUriScheme()
        initRecyclerView()
        initSearchView()
        initLiveDataBookSource()
        initLiveDataGroup()
        initSelectActionBar()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        groupMenu?.findItem(R.id.action_sort)?.subMenu
            ?.setGroupCheckable(R.id.menu_group_sort, true, true)
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_book_source -> startActivity<BookSourceEditActivity>()
            R.id.menu_import_source_qr -> startActivityForResult<QrCodeActivity>(qrRequestCode)
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
            R.id.menu_import_source_local -> FilePicker
                .selectFile(
                    this,
                    importRequestCode,
                    type = "text/*",
                    allowExtensions = arrayOf("txt", "json")
                )
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_sort_manual -> {
                item.isChecked = true
                sort = 0
                initLiveDataBookSource(search_view.query?.toString())
            }
            R.id.menu_sort_auto -> {
                item.isChecked = true
                sort = 2
                initLiveDataBookSource(search_view.query?.toString())
            }
            R.id.menu_sort_pin_yin -> {
                item.isChecked = true
                sort = 3
                initLiveDataBookSource(search_view.query?.toString())
            }
            R.id.menu_sort_url -> {
                item.isChecked = true
                sort = 4
                initLiveDataBookSource(search_view.query?.toString())
            }
            R.id.menu_enabled_group -> {
                search_view.setQuery(getString(R.string.enabled), true)
            }
            R.id.menu_disabled_group -> {
                search_view.setQuery(getString(R.string.disabled), true)
            }
        }
        if (item.groupId == R.id.source_group) {
            search_view.setQuery(item.title, true)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initUriScheme() {
        intent.data?.let {
            when (it.path) {
                "/importonline" -> it.getQueryParameter("src")?.let { url ->
                    Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                    if (url.startsWith("http", false)) {
                        viewModel.importSource(url) { msg ->
                            title_bar.snackbar(msg)
                        }
                    } else {
                        viewModel.importSourceFromFilePath(url) { msg ->
                            title_bar.snackbar(msg)
                        }
                    }
                }
                else -> {
                    toast("格式不对")
                }
            }
        }
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(VerticalDivider(this))
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
        bookSourceLiveDate = when {
            searchKey.isNullOrEmpty() -> {
                App.db.bookSourceDao().liveDataAll()
            }
            searchKey == getString(R.string.enabled) -> {
                App.db.bookSourceDao().liveDataEnabled()
            }
            searchKey == getString(R.string.disabled) -> {
                App.db.bookSourceDao().liveDataDisabled()
            }
            else -> {
                App.db.bookSourceDao().liveDataSearch("%$searchKey%")
            }
        }
        bookSourceLiveDate?.observe(this, Observer { data ->
            val sourceList = when (sort) {
                1 -> data.sortedBy { it.weight }
                2 -> data.sortedBy { it.bookSourceName }
                3 -> data.sortedBy { it.bookSourceUrl }
                else -> data
            }
            val diffResult = DiffUtil
                .calculateDiff(DiffCallBack(ArrayList(adapter.getItems()), sourceList))
            adapter.setItems(sourceList, diffResult)
            upCountView()
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

    private fun initSelectActionBar() {
        select_action_bar.setMainActionText(R.string.delete)
        select_action_bar.inflateMenu(R.menu.book_source_sel)
        select_action_bar.setOnMenuItemClickListener(this)
        select_action_bar.setCallBack(object : SelectActionBar.CallBack {
            override fun selectAll(selectAll: Boolean) {
                if (selectAll) {
                    adapter.selectAll()
                } else {
                    adapter.revertSelection()
                }
            }

            override fun revertSelection() {
                adapter.revertSelection()
            }

            override fun onClickMainAction() {
                this@BookSourceActivity
                    .alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
                        okButton { viewModel.delSelection(adapter.getSelection()) }
                        noButton { }
                    }
                    .show().applyTint()
            }
        })

    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelection())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelection())
            R.id.menu_enable_explore -> viewModel.enableSelectExplore(adapter.getSelection())
            R.id.menu_disable_explore -> viewModel.disableSelectExplore(adapter.getSelection())
            R.id.menu_export_selection -> FilePicker.selectFolder(this, exportRequestCode)
            R.id.menu_check_source -> checkSource()
            R.id.menu_top_sel -> viewModel.topSource(*adapter.getSelection().toTypedArray())
            R.id.menu_bottom_sel -> viewModel.bottomSource(*adapter.getSelection().toTypedArray())
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun checkSource() {
        alert(titleResource = R.string.search_book_key) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view
                    edit_view.setText(CheckSource.keyword)
                }
            }
            okButton {
                editText?.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        CheckSource.keyword = it
                    }
                }
                CheckSource.start(this@BookSourceActivity, adapter.getSelection())
            }
            noButton { }
        }.show().applyTint()
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.source_group)
        groups.sortedWith(Collator.getInstance(java.util.Locale.CHINESE))
            .map {
                groupMenu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
            }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_book_source_on_line) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view
                    edit_view.setFilterValues(cacheUrls)
                    edit_view.delCallBack = {
                        cacheUrls.remove(it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                }
            }
            okButton {
                val text = editText?.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                    viewModel.importSource(it) { msg ->
                        title_bar.snackbar(msg)
                    }
                }
            }
            cancelButton()
        }.show().applyTint()
    }

    override fun upCountView() {
        select_action_bar.upCountView(adapter.getSelection().size, adapter.getActualItemCount())
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
        startActivity<BookSourceEditActivity>(Pair("data", bookSource.bookSourceUrl))
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

    override fun toTop(bookSource: BookSource) {
        viewModel.topSource(bookSource)
    }

    override fun toBottom(bookSource: BookSource) {
        viewModel.bottomSource(bookSource)
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            exportRequestCode -> viewModel.exportSelection(
                adapter.getSelection(),
                File(currentPath)
            )
            importRequestCode -> {
                Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                viewModel.importSourceFromFilePath(currentPath) { msg ->
                    title_bar.snackbar(msg)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> if (resultCode == RESULT_OK) {
                data?.getStringExtra("result")?.let {
                    Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                    viewModel.importSource(it) { msg ->
                        title_bar.snackbar(msg)
                    }
                }
            }
            importRequestCode -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    try {
                        uri.readText(this)?.let {
                            Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE)
                                .show()
                            viewModel.importSource(it) { msg ->
                                title_bar.snackbar(msg)
                            }
                        }
                    } catch (e: Exception) {
                        toast(e.localizedMessage ?: "ERROR")
                    }
                }
            }
            exportRequestCode -> {
                data?.data?.let { uri ->
                    if (uri.toString().isContentPath()) {
                        DocumentFile.fromTreeUri(this, uri)?.let {
                            viewModel.exportSelection(adapter.getSelection(), it)
                        }
                    } else {
                        uri.path?.let {
                            viewModel.exportSelection(adapter.getSelection(), File(it))
                        }
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