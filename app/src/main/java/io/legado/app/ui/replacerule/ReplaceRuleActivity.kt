package io.legado.app.ui.replacerule

import android.annotation.SuppressLint
import android.app.Activity
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
import com.google.android.material.snackbar.Snackbar
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.ItemTouchCallback
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.cancelButton
import io.legado.app.lib.dialogs.customView
import io.legado.app.lib.dialogs.okButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.lib.theme.view.ATEAutoCompleteTextView
import io.legado.app.ui.replacerule.edit.ReplaceEditDialog
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_replace_rule.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.view_search.*
import org.jetbrains.anko.toast


class ReplaceRuleActivity : VMBaseActivity<ReplaceRuleViewModel>(R.layout.activity_replace_rule),
    SearchView.OnQueryTextListener,
    ReplaceRuleAdapter.CallBack {
    override val viewModel: ReplaceRuleViewModel
        get() = getViewModel(ReplaceRuleViewModel::class.java)
    private val importSource = 132
    private lateinit var adapter: ReplaceRuleAdapter
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null
    private var replaceRuleLiveData: LiveData<List<ReplaceRule>>? = null
    private var dataInit = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
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

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add_replace_rule ->
                ReplaceEditDialog().show(supportFragmentManager, "replaceNew")
            R.id.menu_group_manage ->
                GroupManageDialog().show(supportFragmentManager, "groupManage")
            R.id.menu_select_all -> adapter.selectAll()
            R.id.menu_revert_selection -> adapter.revertSelection()
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelectionIds())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelectionIds())
            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelectionIds())
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_import_source_local -> selectFileSys()
            R.id.menu_export_selection -> viewModel.exportSelection(adapter.getSelectionIds())
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ReplaceRuleAdapter(this, this)
        recycler_view.adapter = adapter
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
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

    private fun observeReplaceRuleData(key: String? = null) {
        replaceRuleLiveData?.removeObservers(this)
        dataInit = false
        replaceRuleLiveData = if (key.isNullOrEmpty()) {
            App.db.replaceRuleDao().liveDataAll()
        } else {
            App.db.replaceRuleDao().liveDataSearch(key)
        }
        replaceRuleLiveData?.observe(this, Observer {
            if (dataInit) {
                setResult(Activity.RESULT_OK)
            }
            val diffResult = DiffUtil.calculateDiff(DiffCallBack(adapter.getItems(), it))
            adapter.setItems(it, false)
            diffResult.dispatchUpdatesTo(adapter)
            dataInit = true
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
            .getAsString("replaceRuleUrl")
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_replace_rule_on_line) {
            var editText: ATEAutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view
                    edit_view.setFilterValues(cacheUrls) {
                        cacheUrls.remove(it)
                        aCache.put("replaceRuleUrl", cacheUrls.joinToString(","))
                    }
                }
            }
            okButton {
                val text = editText?.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put("replaceRuleUrl", cacheUrls.joinToString(","))
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

    private fun selectFileSys() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.type = "text/*"//设置类型
            startActivityForResult(intent, importSource)
        } catch (e: Exception) {

        }
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        observeReplaceRuleData("%$newText%")
        return false
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            importSource -> if (resultCode == Activity.RESULT_OK) {
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
                        e.localizedMessage?.let { toast(it) }
                    }
                }
            }
        }
    }

    override fun update(vararg rule: ReplaceRule) {
        viewModel.update(*rule)
    }

    override fun delete(rule: ReplaceRule) {
        viewModel.delete(rule)
    }

    override fun edit(rule: ReplaceRule) {
        ReplaceEditDialog
            .newInstance(rule.id)
            .show(supportFragmentManager, "editReplace")
    }

    override fun toTop(rule: ReplaceRule) {
        viewModel.toTop(rule)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }
}