package io.legado.app.ui.replace

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ActivityReplaceRuleBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.service.help.ReadBook
import io.legado.app.ui.association.ImportReplaceRuleActivity
import io.legado.app.ui.filepicker.FilePicker
import io.legado.app.ui.filepicker.FilePickerDialog
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.replace.edit.ReplaceEditActivity
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.*
import java.io.File

/**
 * 替换规则管理
 */
class ReplaceRuleActivity : VMBaseActivity<ActivityReplaceRuleBinding, ReplaceRuleViewModel>(),
    SearchView.OnQueryTextListener,
    PopupMenu.OnMenuItemClickListener,
    FilePickerDialog.CallBack,
    SelectActionBar.CallBack,
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel by viewModels()
    private val importRecordKey = "replaceRuleRecordKey"
    private val importRequestCode = 132
    private val importRequestCodeQr = 133
    private val exportRequestCode = 234
    private val editActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            setResult(RESULT_OK)
        }
    private lateinit var adapter: ReplaceRuleAdapter
    private lateinit var searchView: SearchView
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var replaceRuleLiveData: LiveData<List<ReplaceRule>>? = null
    private var dataInit = false

    override fun getViewBinding(): ActivityReplaceRuleBinding {
        return ActivityReplaceRuleBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        searchView = binding.titleBar.findViewById(R.id.search_view)
        initRecyclerView()
        initSearchView()
        initSelectActionView()
        observeReplaceRuleData()
        observeGroupData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_rule, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(binding.recyclerView)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this, this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        val dragSelectTouchHelper: DragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        // When this page is opened, it is in selection mode
        dragSelectTouchHelper.activeSlideSelect()

        // Note: need judge selection first, so add ItemTouchHelper after it.
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initSearchView() {
        ATH.setTint(searchView, primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.queryHint = getString(R.string.replace_purify_search)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(this)
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
        delSourceDialog()
    }

    private fun initSelectActionView() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.inflateMenu(R.menu.replace_rule_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun delSourceDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton { viewModel.delSelection(adapter.getSelection()) }
            noButton()
        }.show()
    }

    private fun observeReplaceRuleData(searchKey: String? = null) {
        dataInit = false
        replaceRuleLiveData?.removeObservers(this)
        replaceRuleLiveData = when {
            searchKey.isNullOrEmpty() -> {
                appDb.replaceRuleDao.liveDataAll()
            }
            searchKey.startsWith("group:") -> {
                val key = searchKey.substringAfter("group:")
                appDb.replaceRuleDao.liveDataGroupSearch("%$key%")
            }
            else -> {
                appDb.replaceRuleDao.liveDataSearch("%$searchKey%")
            }
        }.apply {
            observe(this@ReplaceRuleActivity, {
                if (dataInit) {
                    setResult(Activity.RESULT_OK)
                }
                adapter.setItems(it, adapter.diffItemCallBack)
                dataInit = true
            })
        }
    }

    private fun observeGroupData() {
        appDb.replaceRuleDao.liveGroup().observe(this, {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
            }
            upGroupMenu()
        })
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                editActivity.launch(ReplaceEditActivity.startIntent(this))
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")

            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelection())
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_import_source_local -> FilePicker
                .selectFile(this, importRequestCode, allowExtensions = arrayOf("txt", "json"))
            R.id.menu_import_source_qr -> startActivityForResult<QrCodeActivity>(importRequestCodeQr)
            R.id.menu_help -> showHelp()
            else -> if (item.groupId == R.id.replace_group) {
                searchView.setQuery("group:${item.title}", true)
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelection())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelection())
            R.id.menu_export_selection -> FilePicker.selectFolder(this, exportRequestCode)
        }
        return false
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.replace_group)
        groups.map {
            groupMenu?.add(R.id.replace_group, Menu.NONE, Menu.NONE, it)
        }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_replace_rule_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
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
                    startActivity<ImportReplaceRuleActivity> {
                        putExtra("source", it)
                    }
                }
            }
            cancelButton()
        }.show()
    }

    private fun showHelp() {
        val text = String(assets.open("help/replaceRuleHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, text, TextDialog.MD)
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        observeReplaceRuleData(newText)
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            importRequestCode -> {
                data?.data?.let { uri ->
                    kotlin.runCatching {
                        uri.readText(this)?.let {
                            val dataKey = IntentDataHelp.putData(it)
                            startActivity<ImportReplaceRuleActivity> {
                                putExtra("dataKey", dataKey)
                            }
                        }
                    }.onFailure {
                        toastOnUi("readTextError:${it.localizedMessage}")
                    }
                }
            }
            importRequestCodeQr -> {
                data?.getStringExtra("result")?.let {
                    startActivity<ImportReplaceRuleActivity> {
                        putExtra("source", it)
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

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async { ReadBook.contentProcessor?.upReplaceRules() }
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(
            adapter.getSelection().size,
            adapter.itemCount
        )
    }

    override fun update(vararg rule: ReplaceRule) {
        setResult(RESULT_OK)
        viewModel.update(*rule)
    }

    override fun delete(rule: ReplaceRule) {
        setResult(RESULT_OK)
        viewModel.delete(rule)
    }

    override fun edit(rule: ReplaceRule) {
        setResult(RESULT_OK)
        editActivity.launch(ReplaceEditActivity.startIntent(this, rule.id))
    }

    override fun toTop(rule: ReplaceRule) {
        setResult(RESULT_OK)
        viewModel.toTop(rule)
    }

    override fun toBottom(rule: ReplaceRule) {
        setResult(RESULT_OK)
        viewModel.toBottom(rule)
    }

    override fun upOrder() {
        setResult(RESULT_OK)
        viewModel.upOrder()
    }
}