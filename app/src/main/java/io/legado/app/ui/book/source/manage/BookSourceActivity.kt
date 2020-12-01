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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityBookSourceBinding
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.help.CheckSource
import io.legado.app.ui.association.ImportBookSourceActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.filepicker.FilePicker
import io.legado.app.ui.filepicker.FilePickerDialog
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.File
import java.text.Collator

class BookSourceActivity : VMBaseActivity<ActivityBookSourceBinding, BookSourceViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    BookSourceAdapter.CallBack,
    FilePickerDialog.CallBack,
    SelectActionBar.CallBack,
    SearchView.OnQueryTextListener {
    override val viewModel: BookSourceViewModel
        get() = getViewModel(BookSourceViewModel::class.java)
    private val importRecordKey = "bookSourceRecordKey"
    private val qrRequestCode = 101
    private val importRequestCode = 132
    private val exportRequestCode = 65
    private lateinit var adapter: BookSourceAdapter
    private lateinit var searchView: SearchView
    private var bookSourceLiveDate: LiveData<List<BookSource>>? = null
    private var groups = linkedSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var sort = 0
    private var sortAscending = 0
    private var snackBar: Snackbar? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        initRecyclerView()
        initSearchView()
        initLiveDataBookSource()
        initLiveDataGroup()
        initSelectActionBar()
        if (LocalConfig.isFirstOpenBookSources) {
            showHelp()
        }
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
            R.id.menu_share_source -> viewModel.shareSelection(adapter.getSelection())
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
            R.id.menu_import_source_local -> FilePicker
                .selectFile(this, importRequestCode, allowExtensions = arrayOf("txt", "json"))
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_sort_manual -> {
                item.isChecked = true
                sortCheck(0)
                initLiveDataBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_auto -> {
                item.isChecked = true
                sortCheck(1)
                initLiveDataBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_pin_yin -> {
                item.isChecked = true
                sortCheck(2)
                initLiveDataBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_url -> {
                item.isChecked = true
                sortCheck(3)
                initLiveDataBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_time -> {
                item.isChecked = true
                sortCheck(4)
                initLiveDataBookSource(searchView.query?.toString())
            }
            R.id.menu_enabled_group -> {
                searchView.setQuery(getString(R.string.enabled), true)
            }
            R.id.menu_disabled_group -> {
                searchView.setQuery(getString(R.string.disabled), true)
            }
            R.id.menu_help -> showHelp()
        }
        if (item.groupId == R.id.source_group) {
            searchView.setQuery(item.title, true)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        adapter = BookSourceAdapter(this, this)
        binding.recyclerView.adapter = adapter
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.initDragSelectTouchHelperCallback()).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        // When this page is opened, it is in selection mode
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.queryHint = getString(R.string.search_book_source)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(this)
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
        bookSourceLiveDate?.observe(this, { data ->
            val sourceList = when (sortAscending % 2) {
                0 -> when (sort) {
                    1 -> data.sortedBy { it.weight }
                    2 -> data.sortedBy { it.bookSourceName }
                    3 -> data.sortedBy { it.bookSourceUrl }
                    4 -> data.sortedByDescending { it.lastUpdateTime }
                    else -> data
                }
                else -> when (sort) {
                    1 -> data.sortedByDescending { it.weight }
                    2 -> data.sortedByDescending { it.bookSourceName }
                    3 -> data.sortedByDescending { it.bookSourceUrl }
                    4 -> data.sortedBy { it.lastUpdateTime }
                    else -> data.reversed()
                }
            }
            val diffResult = DiffUtil
                .calculateDiff(DiffCallBack(ArrayList(adapter.getItems()), sourceList))
            adapter.setItems(sourceList, diffResult)
            upCountView()
        })
    }

    private fun showHelp() {
        val text = String(assets.open("help/bookSourcesHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, text, TextDialog.MD)
    }

    private fun sortCheck(sortId: Int) {
        if (sort == sortId) {
            sortAscending += 1
        } else {
            sortAscending = 0
            sort = sortId
        }
    }

    private fun initLiveDataGroup() {
        App.db.bookSourceDao().liveGroup().observe(this, {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            upGroupMenu()
        })
    }

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
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton { viewModel.delSelection(adapter.getSelection()) }
            noButton()
        }.show()
    }

    private fun initSelectActionBar() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.inflateMenu(R.menu.book_source_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelection())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelection())
            R.id.menu_enable_explore -> viewModel.enableSelectExplore(adapter.getSelection())
            R.id.menu_disable_explore -> viewModel.disableSelectExplore(adapter.getSelection())
            R.id.menu_check_source -> checkSource()
            R.id.menu_top_sel -> viewModel.topSource(*adapter.getSelection().toTypedArray())
            R.id.menu_bottom_sel -> viewModel.bottomSource(*adapter.getSelection().toTypedArray())
            R.id.menu_add_group -> selectionAddToGroups()
            R.id.menu_remove_group -> selectionRemoveFromGroups()
            R.id.menu_export_selection -> FilePicker.selectFolder(this, exportRequestCode)
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun checkSource() {
        alert(titleResource = R.string.search_book_key) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = findViewById(R.id.edit_view)
                    editText?.setText(CheckSource.keyword)
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
            noButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun selectionAddToGroups() {
        alert(titleResource = R.string.add_group) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = findViewById(R.id.edit_view)
                    editText?.setHint(R.string.group_name)
                    editText?.setFilterValues(groups.toList())
                    editText?.dropDownHeight = 180.dp
                }
            }
            okButton {
                editText?.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionAddToGroups(adapter.getSelection(), it)
                    }
                }
            }
            cancelButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun selectionRemoveFromGroups() {
        alert(titleResource = R.string.remove_group) {
            var editText: AutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = findViewById(R.id.edit_view)
                    editText?.setHint(R.string.group_name)
                    editText?.setFilterValues(groups.toList())
                    editText?.dropDownHeight = 180.dp
                }
            }
            okButton {
                editText?.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionRemoveFromGroups(adapter.getSelection(), it)
                    }
                }
            }
            cancelButton()
        }.show()
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
                    editText = findViewById(R.id.edit_view)
                    editText?.setFilterValues(cacheUrls)
                    editText?.delCallBack = {
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
                    startActivity<ImportBookSourceActivity>(Pair("source", it))
                }
            }
            cancelButton()
        }.show()
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.CHECK_SOURCE) { msg ->
            snackBar?.setText(msg) ?: let {
                snackBar = Snackbar
                    .make(binding.root, msg, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.cancel) {
                        CheckSource.stop(this)
                    }.apply { show() }
            }
        }
        observeEvent<Int>(EventBus.CHECK_SOURCE_DONE) {
            snackBar?.dismiss()
            snackBar = null
            groups.map { group ->
                if (group.contains("失效")) {
                    searchView.setQuery("失效", true)
                    toast("发现有失效书源，已为您自动筛选！")
                }
            }
        }
    }

    override fun upCountView() {
        binding.selectActionBar
            .upCountView(adapter.getSelection().size, adapter.getActualItemCount())
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            qrRequestCode -> if (resultCode == RESULT_OK) {
                data?.getStringExtra("result")?.let {
                    startActivity<ImportBookSourceActivity>("source" to it)
                }
            }
            importRequestCode -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    try {
                        uri.readText(this)?.let {
                            val dataKey = IntentDataHelp.putData(it)
                            startActivity<ImportBookSourceActivity>("dataKey" to dataKey)
                        }
                    } catch (e: Exception) {
                        toast("readTextError:${e.localizedMessage}")
                    }
                }
            }
            exportRequestCode -> {
                data?.data?.let { uri ->
                    if (uri.isContentScheme()) {
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
        if (searchView.query.isNullOrEmpty()) {
            super.finish()
        } else {
            searchView.setQuery("", true)
        }
    }

}