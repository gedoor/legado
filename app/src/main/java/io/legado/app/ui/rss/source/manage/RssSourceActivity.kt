package io.legado.app.ui.rss.source.manage

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssSource
import io.legado.app.help.ItemTouchCallback
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.dialogs.*
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.lib.theme.view.ATEAutoCompleteTextView
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.qrcode.QrCodeActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_rss_source.*
import kotlinx.android.synthetic.main.dialog_edit_text.view.*
import kotlinx.android.synthetic.main.view_search.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.io.FileNotFoundException


class RssSourceActivity : VMBaseActivity<RssSourceViewModel>(R.layout.activity_rss_source),
    PopupMenu.OnMenuItemClickListener,
    FileChooserDialog.CallBack,
    RssSourceAdapter.CallBack {

    override val viewModel: RssSourceViewModel
        get() = getViewModel(RssSourceViewModel::class.java)
    private val importRecordKey = "rssSourceRecordKey"
    private val qrRequestCode = 101
    private val importSource = 13141
    private lateinit var adapter: RssSourceAdapter
    private var sourceLiveData: LiveData<List<RssSource>>? = null
    private var groups = hashSetOf<String>()
    private var groupMenu: SubMenu? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        initLiveDataGroup()
        initLiveDataSource()
        initViewEvent()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        groupMenu = menu?.findItem(R.id.menu_group)?.subMenu
        upGroupMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> startActivity<RssSourceEditActivity>()
            R.id.menu_import_source_local -> selectFileSys()
            R.id.menu_import_source_onLine -> showImportDialog()
            R.id.menu_import_source_qr -> startActivityForResult<QrCodeActivity>(qrRequestCode)
            R.id.menu_group_manage -> GroupManageDialog()
                .show(supportFragmentManager, "rssGroupManage")
        }
        if (item.groupId == R.id.source_group) {
            search_view.setQuery(item.title, true)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_select_all -> adapter.selectAll()
            R.id.menu_revert_selection -> adapter.revertSelection()
            R.id.menu_enable_selection -> viewModel.enableSelection(adapter.getSelection())
            R.id.menu_disable_selection -> viewModel.disableSelection(adapter.getSelection())
            R.id.menu_del_selection -> viewModel.delSelection(adapter.getSelection())
            R.id.menu_export_selection -> viewModel.exportSelection(adapter.getSelection())
            R.id.menu_check_source -> {
            }
        }
        return true
    }

    private fun initRecyclerView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(recycler_view.getVerticalDivider())
        adapter = RssSourceAdapter(this, this)
        recycler_view.adapter = adapter
        val itemTouchCallback = ItemTouchCallback()
        itemTouchCallback.onItemTouchCallbackListener = adapter
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recycler_view)
    }

    private fun initSearchView() {
        ATH.setTint(search_view, primaryTextColor)
        search_view.onActionViewExpanded()
        search_view.queryHint = getString(R.string.search_rss_source)
        search_view.clearFocus()
        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                initLiveDataSource(newText)
                return false
            }
        })
    }

    private fun initLiveDataGroup() {
        App.db.rssSourceDao().liveGroup().observe(this, Observer {
            groups.clear()
            it.map { group ->
                groups.addAll(group.splitNotBlank(",", ";"))
            }
            upGroupMenu()
        })
    }

    private fun initViewEvent() {
        select_action_bar.setMainActionText(R.string.delete)
        select_action_bar.inflateMenu(R.menu.rss_source_sel)
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
                this@RssSourceActivity
                    .alert(titleResource = R.string.sure, messageResource = R.string.sure_del) {
                        okButton { viewModel.delSelection(adapter.getSelection()) }
                        noButton { }
                    }
                    .show().applyTint()
            }
        })
    }

    private fun upGroupMenu() {
        groupMenu?.removeGroup(R.id.source_group)
        groups.map {
            groupMenu?.add(R.id.source_group, Menu.NONE, Menu.NONE, it)
        }
    }

    private fun initLiveDataSource(key: String? = null) {
        sourceLiveData?.removeObservers(this)
        sourceLiveData =
            if (key.isNullOrBlank()) {
                App.db.rssSourceDao().liveAll()
            } else {
                App.db.rssSourceDao().liveSearch("%$key%")
            }
        sourceLiveData?.observe(this, Observer {
            val diffResult = DiffUtil
                .calculateDiff(DiffCallBack(adapter.getItems(), it))
            adapter.setItems(it, false)
            diffResult.dispatchUpdatesTo(adapter)
            upCountView()
        })
    }

    override fun upCountView() {
        select_action_bar.upCountView(adapter.getSelection().size, adapter.getActualItemCount())
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_book_source_on_line) {
            var editText: ATEAutoCompleteTextView? = null
            customView {
                layoutInflater.inflate(R.layout.dialog_edit_text, null).apply {
                    editText = edit_view
                    edit_view.setFilterValues(cacheUrls) {
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

    private fun selectFileSys() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "text/*"//设置类型
            startActivityForResult(intent, importSource)
        } catch (e: Exception) {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    selectFile()
                }
                .request()
        }
    }

    private fun selectFile() {
        FileChooserDialog.show(
            supportFragmentManager, importSource,
            allowExtensions = arrayOf("txt", "json")
        )
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        if (requestCode == importSource) {
            Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE).show()
            viewModel.importSourceFromFilePath(currentPath) { msg ->
                title_bar.snackbar(msg)
            }
        }
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
                    } catch (e: FileNotFoundException) {
                        PermissionsCompat.Builder(this)
                            .addPermissions(
                                Permissions.READ_EXTERNAL_STORAGE,
                                Permissions.WRITE_EXTERNAL_STORAGE
                            )
                            .rationale(R.string.bg_image_per)
                            .onGranted {
                                selectFileSys()
                            }
                            .request()
                    } catch (e: Exception) {
                        toast(e.localizedMessage ?: "ERROR")
                    }
                }
            }
            qrRequestCode -> if (resultCode == RESULT_OK) {
                data?.getStringExtra("result")?.let {
                    Snackbar.make(title_bar, R.string.importing, Snackbar.LENGTH_INDEFINITE)
                    viewModel.importSource(it) { msg ->
                        title_bar.snackbar(msg)
                    }
                }
            }
        }
    }

    override fun del(source: RssSource) {
        viewModel.del(source)
    }

    override fun edit(source: RssSource) {
        startActivity<RssSourceEditActivity>(Pair("data", source.sourceUrl))
    }

    override fun update(vararg source: RssSource) {
        viewModel.update(*source)
    }

    override fun toTop(source: RssSource) {
        viewModel.topSource(source)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

}