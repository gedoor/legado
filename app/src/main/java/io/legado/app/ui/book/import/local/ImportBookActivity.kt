package io.legado.app.ui.book.import.local

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.book.import.BaseImportBookActivity
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 导入本地书籍界面
 */
class ImportBookActivity : BaseImportBookActivity<ImportBookViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    ImportBookAdapter.CallBack,
    SelectActionBar.CallBack {

    override val viewModel by viewModels<ImportBookViewModel>()
    private val adapter by lazy { ImportBookAdapter(this, this) }
    private var scanDocJob: Job? = null

    private val selectFolder = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            AppConfig.importBookPath = uri.toString()
            initRootDoc(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        searchView.queryHint = getString(R.string.screen) + " • " + getString(R.string.local_book)
        onBackPressedDispatcher.addCallback(this) {
            if (!goBackDir()) {
                finish()
            }
        }
        lifecycleScope.launch {
            initView()
            initEvent()
            if (setBookStorage() && AppConfig.importBookPath.isNullOrBlank()) {
                AppConfig.importBookPath = AppConfig.defaultBookTreeUri
            }
            initData()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.import_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_sort_name)?.isChecked = viewModel.sort == 0
        menu.findItem(R.id.menu_sort_size)?.isChecked = viewModel.sort == 1
        menu.findItem(R.id.menu_sort_time)?.isChecked = viewModel.sort == 2
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_folder -> selectFolder.launch()
            R.id.menu_scan_folder -> scanFolder()
            R.id.menu_import_file_name -> alertImportFileName()
            R.id.menu_sort_name -> upSort(0)
            R.id.menu_sort_size -> upSort(1)
            R.id.menu_sort_time -> upSort(2)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection -> viewModel.deleteDoc(adapter.selectedUris) {
                adapter.removeSelection()
            }
        }
        return false
    }

    override fun selectAll(selectAll: Boolean) {
        adapter.selectAll(selectAll)
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClickSelectBarMainAction() {
        viewModel.addToBookshelf(adapter.selectedUris) {
            adapter.selectedUris.clear()
            adapter.notifyDataSetChanged()
        }
    }

    private fun initView() {
        binding.layTop.setBackgroundColor(backgroundColor)
        binding.tvEmptyMsg.setText(R.string.empty_msg_import_book)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.selectActionBar.setMainActionText(R.string.add_to_bookshelf)
        binding.selectActionBar.inflateMenu(R.menu.import_book_sel)
        binding.selectActionBar.setOnMenuItemClickListener(this)
        binding.selectActionBar.setCallBack(this)
    }

    private fun initEvent() {
        binding.tvGoBack.setOnClickListener {
            goBackDir()
        }
    }

    private fun initData() {
        viewModel.dataFlowStart = {
            initRootDoc()
        }
        lifecycleScope.launch {
            viewModel.dataFlow.conflate().collect { docs ->
                adapter.setItems(docs)
            }
        }
    }

    private fun initRootDoc(changedFolder: Boolean = false) {
        if (viewModel.rootDoc != null && !changedFolder) {
            upPath()
        } else {
            val lastPath = AppConfig.importBookPath
            if (lastPath.isNullOrBlank()) {
                binding.tvEmptyMsg.visible()
                selectFolder.launch()
            } else {
                val rootUri = if (lastPath.isUri()) {
                    Uri.parse(lastPath)
                } else {
                    Uri.fromFile(File(lastPath))
                }
                when {
                    rootUri.isContentScheme() -> {
                        kotlin.runCatching {
                            val doc = DocumentFile.fromTreeUri(this, rootUri)
                            if (doc == null || doc.name.isNullOrEmpty()) {
                                binding.tvEmptyMsg.visible()
                                selectFolder.launch()
                            } else {
                                viewModel.subDocs.clear()
                                viewModel.rootDoc = FileDoc.fromDocumentFile(doc)
                                upDocs(viewModel.rootDoc!!)
                            }
                        }.onFailure {
                            binding.tvEmptyMsg.visible()
                            selectFolder.launch()
                        }
                    }
                    else -> initRootPath(rootUri.path!!)
                }
            }
        }
    }

    private fun initRootPath(path: String) {
        binding.tvEmptyMsg.visible()
        PermissionsCompat.Builder()
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                kotlin.runCatching {
                    viewModel.rootDoc = FileDoc.fromFile(File(path))
                    viewModel.subDocs.clear()
                    upPath()
                }.onFailure {
                    binding.tvEmptyMsg.visible()
                    selectFolder.launch()
                }
            }
            .request()
    }

    private fun upSort(sort: Int) {
        viewModel.sort = sort
        putPrefInt(PreferKey.localBookImportSort, sort)
        if (scanDocJob?.isActive != true) {
            viewModel.dataCallback?.setItems(adapter.getItems())
        }
    }

    @Synchronized
    private fun upPath() {
        binding.tvGoBack.isEnabled = viewModel.subDocs.isNotEmpty()
        viewModel.rootDoc?.let {
            scanDocJob?.cancel()
            upDocs(it)
        }
    }

    private fun upDocs(rootDoc: FileDoc) {
        binding.tvEmptyMsg.gone()
        var path = rootDoc.name + File.separator
        var lastDoc = rootDoc
        for (doc in viewModel.subDocs) {
            lastDoc = doc
            path = path + doc.name + File.separator
        }
        binding.tvPath.text = path
        adapter.selectedUris.clear()
        adapter.clearItems()
        viewModel.loadDoc(lastDoc)
    }

    /**
     * 扫描当前文件夹及所有子文件夹
     */
    private fun scanFolder() {
        viewModel.rootDoc?.let { doc ->
            adapter.clearItems()
            val lastDoc = viewModel.subDocs.lastOrNull() ?: doc
            binding.refreshProgressBar.isAutoLoading = true
            scanDocJob?.cancel()
            scanDocJob = lifecycleScope.launch(IO) {
                viewModel.scanDoc(lastDoc, true) {
                    withContext(Main) {
                        binding.refreshProgressBar.isAutoLoading = false
                    }
                }
            }
        }
    }

    private fun alertImportFileName() {
        alert(R.string.import_file_name) {
            setMessage("""使用js处理文件名变量src，将书名作者分别赋值到变量name author""")
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "js"
                editView.setText(AppConfig.bookImportFileName)
            }
            customView { alertBinding.root }
            okButton {
                AppConfig.bookImportFileName = alertBinding.editView.text?.toString()
            }
            cancelButton()
        }
    }

    @Synchronized
    override fun nextDoc(fileDoc: FileDoc) {
        viewModel.subDocs.add(fileDoc)
        upPath()
    }

    @Synchronized
    private fun goBackDir(): Boolean {
        return if (viewModel.subDocs.isNotEmpty()) {
            viewModel.subDocs.removeAt(viewModel.subDocs.lastIndex)
            upPath()
            true
        } else {
            false
        }
    }

    override fun onSearchTextChange(newText: String?) {
        viewModel.updateCallBackFlow(newText)
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(adapter.selectedUris.size, adapter.checkableCount)
    }

    override fun startRead(fileDoc: FileDoc) {
        if (!ArchiveUtils.isArchive(fileDoc.name)) {
            startReadBook(fileDoc.toString())
        } else {
            onArchiveFileClick(fileDoc)
        }
    }

}
