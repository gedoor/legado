package io.legado.app.ui.replacerule

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
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.ItemTouchCallback
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.ui.replacerule.edit.ReplaceEditDialog
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.ui.widget.text.AutoCompleteTextView
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_replace_rule.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.view_search.*
import org.jetbrains.anko.toast
import java.io.File


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule),
    SearchView.OnQueryTextListener,
    PopupMenu.OnMenuItemClickListener,
    FileChooserDialog.CallBack,
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)
    private val importRecordKey = "replaceRuleRecordKey"
    private val importRequestCode = 132
    private val exportRequestCode = 65
    private lateinit var adapter: ReplaceRuleAdapter
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var replaceRuleLiveData: LiveData<List<ReplaceRule>>? = null
    private var dataInit = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initUriScheme()
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

    private fun initUriScheme() {
        intent.data?.let {
            when (it.path) {
                "/importonline" -> it.getQueryParameter("src")?.let { url ->
                    Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                    if (url.startsWith("http", false)){
                        viewModel.importSource(url) { msg ->
                            title_bar.snackbar(msg)
                        }
                    }
                    else{
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
        adapter = ReplaceRuleAdapter(this, this)
        recycler_view.adapter = adapter
        recycler_view.addItemDecoration(VerticalDivider(this))
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.replace_purify_search)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(this)
    }

    private fun initSelectActionView() {
        select_action_bar.setMainActionText(R.string.delete)
        select_action_bar.inflateMenu(R.menu.replace_rule_sel)
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
                this@ReplaceRuleActivity
                    .alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
                        okButton { viewModel.delSelection(adapter.getSelection()) }
                        noButton { }
                    }
                    .show().applyTint()
            }
        })
    }

    private fun observeReplaceRuleData(key: String? = null) {
        dataInit = false
        replaceRuleLiveData?.removeObservers(this)
        replaceRuleLiveData = if (key.isNullOrEmpty()) {
            App.db.replaceRuleDao().liveDataAll()
        } else {
            App.db.replaceRuleDao().liveDataSearch(key)
        }
        replaceRuleLiveData?.observe(this, Observer {
            if (dataInit) {
                setResult(Activity.RESULT_OK)
            }
            val diffResult =
                DiffUtil.calculateDiff(DiffCallBack(ArrayList(adapter.getItems()), it))
            adapter.setItems(it, diffResult)
            dataInit = true
            upCountView()
        })
    }

    private fun observeGroupData() {
        App.db.replaceRuleDao().liveGroup().observe(this, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupMenu()
        })
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                ReplaceEditDialog().show(supportFragmentManager, "replaceNew")
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")

            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelection())
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_import_source_local -> FilePicker
                .selectFile(
                    this,
                    importRequestCode,
                    type = "text/*",
                    allowExtensions = arrayOf("txt", "json")
                )
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
        groupMenu?.removeGroup(R.id.source_group)
        groups.map {
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
        alert(titleResource = R.string.import_replace_rule_on_line) {
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

    override fun onQueryTextChange(newText: String?): Boolean {
        observeReplaceRuleData("%$newText%")
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            importRequestCode -> {
                Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
                viewModel.importSource(File(currentPath).readText()) { msg ->
                    title_bar.snackbar(msg)
                }
            }
            exportRequestCode -> viewModel.exportSelection(
                adapter.getSelection(),
                File(currentPath)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
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
            exportRequestCode -> if (resultCode == RESULT_OK) {
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

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async { BookHelp.upReplaceRules() }
    }

    override fun upCountView() {
        select_action_bar.upCountView(adapter.getSelection().size, adapter.getActualItemCount())
    }

    override fun update(vararg rule: ReplaceRule) {
        setResult(Activity.RESULT_OK)
        viewModel.update(*rule)
    }

    override fun delete(rule: ReplaceRule) {
        setResult(Activity.RESULT_OK)
        viewModel.delete(rule)
    }

    override fun edit(rule: ReplaceRule) {
        setResult(Activity.RESULT_OK)
        ReplaceEditDialog.show(supportFragmentManager, rule.id)
    }

    override fun toTop(rule: ReplaceRule) {
        setResult(Activity.RESULT_OK)
        viewModel.toTop(rule)
    }

    override fun upOrder() {
        setResult(Activity.RESULT_OK)
        viewModel.upOrder()
    }
}