package io.legado.app.ui.book.source.manage

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityBookSourceBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.model.Debug
import io.legado.app.service.help.CheckSource
import io.legado.app.ui.association.ImportBookSourceDialog
import io.legado.app.ui.book.source.debug.BookSourceDebugActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.document.FilePickerParam
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.math.min

class BookSourceActivity : VMBaseActivity<ActivityBookSourceBinding, BookSourceViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    BookSourceAdapter.CallBack,
    SelectActionBar.CallBack,
    SearchView.OnQueryTextListener {
    override val binding by viewBinding(ActivityBookSourceBinding::inflate)
    override val viewModel by viewModels<BookSourceViewModel>()
    private val importRecordKey = "bookSourceRecordKey"
    private lateinit var adapter: BookSourceAdapter
    private lateinit var searchView: SearchView
    private var sourceFlowJob: Job? = null
    private val groups = linkedSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var sort = Sort.Default
    private var sortAscending = true
    private var snackBar: Snackbar? = null
    private val qrResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        ImportBookSourceDialog.start(supportFragmentManager, it)
    }
    private val importDoc = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            uri.readText(this)?.let {
                ImportBookSourceDialog.start(supportFragmentManager, it)
            }
        } catch (e: Exception) {
            toastOnUi("readTextError:${e.localizedMessage}")
        }
    }
    private val exportDir = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            DocumentFile.fromTreeUri(this, uri)?.let {
                viewModel.exportSelection(adapter.selection, it)
            }
        } else {
            uri.path?.let {
                viewModel.exportSelection(adapter.selection, File(it))
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        initRecyclerView()
        initSearchView()
        upBookSource()
        initLiveDataGroup()
        initSelectActionBar()
        if (!LocalConfig.bookSourcesHelpVersionIsLast) {
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
            R.id.menu_import_qr -> qrResult.launch(null)
            R.id.menu_share_source -> viewModel.shareSelection(adapter.selection) {
                startActivity(Intent.createChooser(it, getString(R.string.share_selected_source)))
            }
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
            R.id.menu_import_local -> importDoc.launch(
                FilePickerParam(
                    mode = FilePicker.FILE,
                    allowExtensions = arrayOf("txt", "json")
                )
            )
            R.id.menu_import_onLine -> showImportDialog()
            R.id.menu_sort_manual -> {
                item.isChecked = true
                sortCheck(Sort.Default)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_auto -> {
                item.isChecked = true
                sortCheck(Sort.Weight)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_name -> {
                item.isChecked = true
                sortCheck(Sort.Name)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_url -> {
                item.isChecked = true
                sortCheck(Sort.Url)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_time -> {
                item.isChecked = true
                sortCheck(Sort.Update)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_respondTime -> {
                item.isChecked = true
                sortCheck(Sort.Respond)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_sort_enable -> {
                item.isChecked = true
                sortCheck(Sort.Enable)
                upBookSource(searchView.query?.toString())
            }
            R.id.menu_enabled_group -> {
                searchView.setQuery(getString(R.string.enabled), true)
            }
            R.id.menu_disabled_group -> {
                searchView.setQuery(getString(R.string.disabled), true)
            }
            R.id.menu_group_login -> {
                searchView.setQuery(getString(R.string.need_login), true)
            }
            R.id.menu_help -> showHelp()
        }
        if (item.groupId == R.id.source_group) {
            searchView.setQuery("group:${item.title}", true)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        adapter = BookSourceAdapter(this, this)
        binding.recyclerView.adapter = adapter
        // When this page is opened, it is in selection mode
        val dragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.queryHint = getString(R.string.search_book_source)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(this)
    }

    private fun upBookSource(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = launch {
            when {
                searchKey.isNullOrEmpty() -> {
                    appDb.bookSourceDao.flowAll()
                }
                searchKey == getString(R.string.enabled) -> {
                    appDb.bookSourceDao.flowEnabled()
                }
                searchKey == getString(R.string.disabled) -> {
                    appDb.bookSourceDao.flowDisabled()
                }
                searchKey == getString(R.string.need_login) -> {
                    appDb.bookSourceDao.flowLogin()
                }
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.bookSourceDao.flowGroupSearch("%$key%")
                }
                else -> {
                    appDb.bookSourceDao.flowSearch("%$searchKey%")
                }
            }.collect { data ->
                val sourceList =
                    if (sortAscending) when (sort) {
                        Sort.Weight -> data.sortedBy { it.weight }
                        Sort.Name -> data.sortedWith { o1, o2 ->
                            o1.bookSourceName.cnCompare(o2.bookSourceName)
                        }
                        Sort.Url -> data.sortedBy { it.bookSourceUrl }
                        Sort.Update -> data.sortedByDescending { it.respondTime }
                        Sort.Respond -> data.sortedByDescending { it.respondTime }
                        Sort.Enable -> data.sortedWith { o1, o2 ->
                            var sort = -o1.enabled.compareTo(o2.enabled)
                            if (sort == 0) {
                                sort = o1.bookSourceName.cnCompare(o2.bookSourceName)
                            }
                            sort
                        }
                        else -> data
                    }
                    else when (sort) {
                        Sort.Weight -> data.sortedByDescending { it.weight }
                        Sort.Name -> data.sortedWith { o1, o2 ->
                            o2.bookSourceName.cnCompare(o1.bookSourceName)
                        }
                        Sort.Url -> data.sortedByDescending { it.bookSourceUrl }
                        Sort.Respond -> data.sortedBy { it.respondTime }
                        Sort.Enable -> data.sortedWith { o1, o2 ->
                            var sort = o1.enabled.compareTo(o2.enabled)
                            if (sort == 0) {
                                sort = o1.bookSourceName.cnCompare(o2.bookSourceName)
                            }
                            sort
                        }
                        else -> data.reversed()
                    }
                adapter.setItems(sourceList, adapter.diffItemCallback)
            }
        }
    }

    private fun showHelp() {
        val text = String(assets.open("help/SourceMBookHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, text, TextDialog.MD)
    }

    private fun sortCheck(sort: Sort) {
        if (this.sort == sort) {
            sortAscending = !sortAscending
        } else {
            sortAscending = true
            this.sort = sort
        }
    }

    private fun initLiveDataGroup() {
        launch {
            appDb.bookSourceDao.flowGroup()
                .collect {
                    groups.clear()
                    it.forEach { group ->
                        groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                    }
                    upGroupMenu()
                }
        }
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
            okButton { viewModel.delSelection(adapter.selection) }
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
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.selection)
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.selection)
            R.id.menu_enable_explore -> viewModel.enableSelectExplore(adapter.selection)
            R.id.menu_disable_explore -> viewModel.disableSelectExplore(adapter.selection)
            R.id.menu_check_source -> checkSource()
            R.id.menu_top_sel -> viewModel.topSource(*adapter.selection.toTypedArray())
            R.id.menu_bottom_sel -> viewModel.bottomSource(*adapter.selection.toTypedArray())
            R.id.menu_add_group -> selectionAddToGroups()
            R.id.menu_remove_group -> selectionRemoveFromGroups()
            R.id.menu_export_selection -> exportDir.launch(null)
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun checkSource() {
        alert(titleResource = R.string.search_book_key) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "search word"
                editView.setText(CheckSource.keyword)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        CheckSource.keyword = it
                    }
                }
                CheckSource.start(this@BookSourceActivity, adapter.selection)
                checkMessageRefreshJob().start()
            }
            noButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun selectionAddToGroups() {
        alert(titleResource = R.string.add_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dp
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionAddToGroups(adapter.selection, it)
                    }
                }
            }
            cancelButton()
        }.show()
    }

    @SuppressLint("InflateParams")
    private fun selectionRemoveFromGroups() {
        alert(titleResource = R.string.remove_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dp
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionRemoveFromGroups(adapter.selection, it)
                    }
                }
            }
            cancelButton()
        }.show()
    }

    private fun upGroupMenu() = groupMenu?.let { menu ->
        menu.removeGroup(R.id.source_group)
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.map {
            menu.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importRecordKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    ImportBookSourceDialog.start(supportFragmentManager, it)
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
                        Debug.finishChecking()
                        adapter.notifyItemRangeChanged(
                            0,
                            adapter.itemCount,
                            bundleOf(Pair("checkSourceMessage", null))
                        )
                    }.apply { show() }
            }
        }
        observeEvent<Int>(EventBus.CHECK_SOURCE_DONE) {
            snackBar?.dismiss()
            snackBar = null
            groups.map { group ->
                if (group.contains("失效")) {
                    searchView.setQuery("失效", true)
                    toastOnUi("发现有失效书源，已为您自动筛选！")
                }
            }
        }
    }

    private fun checkMessageRefreshJob(): Job {
        val firstIndex = adapter.getItems().indexOf(adapter.selection.firstOrNull())
        val lastIndex = adapter.getItems().indexOf(adapter.selection.lastOrNull())
        var refreshCount = 0
        Debug.isChecking = firstIndex >= 0 && lastIndex >= 0
        return async(start = CoroutineStart.LAZY) {
            flow {
                while (true) {
                    refreshCount += 1
                    emit(refreshCount)
                    delay(300L)
                }
            }.collect {
                adapter.notifyItemRangeChanged(firstIndex, lastIndex + 1, bundleOf(Pair("checkSourceMessage", null)))
                if (!Debug.isChecking || (refreshCount > (600 * (lastIndex + 1 - firstIndex) / min(AppConfig.threadCount,8)))) {
                    Debug.finishChecking()
                    this.cancel()
                }
            }
        }
    }

    override fun upCountView() {
        binding.selectActionBar
            .upCountView(adapter.selection.size, adapter.itemCount)
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            upBookSource(it)
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
        startActivity<BookSourceEditActivity> {
            putExtra("data", bookSource.bookSourceUrl)
        }
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

    override fun debug(bookSource: BookSource) {
        startActivity<BookSourceDebugActivity> {
            putExtra("key", bookSource.bookSourceUrl)
        }
    }

    override fun finish() {
        if (searchView.query.isNullOrEmpty()) {
            super.finish()
        } else {
            searchView.setQuery("", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!Debug.isChecking) {
            Debug.debugMessageMap.clear()
        }
    }

    enum class Sort {
        Default, Name, Url, Weight, Update, Enable, Respond
    }
}