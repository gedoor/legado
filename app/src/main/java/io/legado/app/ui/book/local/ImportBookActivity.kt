package io.legado.app.ui.book.local

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.appDb
import io.legado.app.databinding.ActivityImportBookBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.ui.document.FilePicker
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * 导入本地书籍界面
 */
class ImportBookActivity : VMBaseActivity<ActivityImportBookBinding, ImportBookViewModel>(),
    PopupMenu.OnMenuItemClickListener,
    ImportBookAdapter.CallBack,
    SelectActionBar.CallBack {
    private var rootDoc: DocumentFile? = null
    private val subDocs = arrayListOf<DocumentFile>()
    private lateinit var adapter: ImportBookAdapter
    private var localUriLiveData: LiveData<List<String>>? = null
    private var sdPath = FileUtils.getSdCardPath()
    private var path = sdPath
    private val selectFolder = registerForActivityResult(FilePicker()) { uri ->
        uri ?: return@registerForActivityResult
        if (uri.isContentScheme()) {
            AppConfig.importBookPath = uri.toString()
            initRootDoc()
        } else {
            uri.path?.let { path ->
                AppConfig.importBookPath = path
                initRootDoc()
            }
        }
    }

    override val viewModel: ImportBookViewModel
            by viewModels()

    override fun getViewBinding(): ActivityImportBookBinding {
        return ActivityImportBookBinding.inflate(layoutInflater)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initEvent()
        initData()
        initRootDoc()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.import_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_folder -> selectFolder.launch(null)
            R.id.menu_scan_folder -> scanFolder()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection ->
                viewModel.deleteDoc(adapter.selectedUris) {
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

    override fun onClickMainAction() {
        viewModel.addToBookshelf(adapter.selectedUris) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun initView() {
        binding.layTop.setBackgroundColor(backgroundColor)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ImportBookAdapter(this, this)
        binding.recyclerView.adapter = adapter
        binding.selectActionBar.setMainActionText(R.string.add_to_shelf)
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
        localUriLiveData?.removeObservers(this)
        localUriLiveData = appDb.bookDao.observeLocalUri()
        localUriLiveData?.observe(this, {
            adapter.upBookHas(it)
        })
    }

    private fun initRootDoc() {
        val lastPath = AppConfig.importBookPath
        when {
            lastPath.isNullOrEmpty() -> {
                binding.tvEmptyMsg.visible()
                selectFolder.launch(null)
            }
            lastPath.isContentScheme() -> {
                val rootUri = Uri.parse(lastPath)
                rootDoc = DocumentFile.fromTreeUri(this, rootUri)
                if (rootDoc == null) {
                    binding.tvEmptyMsg.visible()
                    selectFolder.launch(null)
                } else {
                    subDocs.clear()
                    upPath()
                }
            }
            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> {
                binding.tvEmptyMsg.visible()
                selectFolder.launch(null)
            }
            else -> {
                binding.tvEmptyMsg.visible()
                PermissionsCompat.Builder(this)
                    .addPermissions(*Permissions.Group.STORAGE)
                    .rationale(R.string.tip_perm_request_storage)
                    .onGranted {
                        rootDoc = null
                        subDocs.clear()
                        path = lastPath
                        upPath()
                    }
                    .request()
            }
        }
    }

    @Synchronized
    private fun upPath() {
        rootDoc?.let {
            upDocs(it)
        } ?: upFiles()
    }

    private fun upDocs(rootDoc: DocumentFile) {
        binding.tvEmptyMsg.gone()
        var path = rootDoc.name.toString() + File.separator
        var lastDoc = rootDoc
        for (doc in subDocs) {
            lastDoc = doc
            path = path + doc.name + File.separator
        }
        binding.tvPath.text = path
        adapter.selectedUris.clear()
        adapter.clearItems()
        launch(IO) {
            val docList = DocumentUtils.listFiles(this@ImportBookActivity, lastDoc.uri)
            for (i in docList.lastIndex downTo 0) {
                val item = docList[i]
                if (item.name.startsWith(".")) {
                    docList.removeAt(i)
                } else if (!item.isDir
                    && !item.name.endsWith(".txt", true)
                    && !item.name.endsWith(".epub", true)
                ) {
                    docList.removeAt(i)
                }
            }
            docList.sortWith(compareBy({ !it.isDir }, { it.name }))
            withContext(Main) {
                adapter.setItems(docList)
            }
        }
    }

    private fun upFiles() {
        binding.tvEmptyMsg.gone()
        binding.tvPath.text = path.replace(sdPath, "SD")
        val docList = arrayListOf<DocItem>()
        File(path).listFiles()?.forEach {
            if (it.isDirectory) {
                if (!it.name.startsWith("."))
                    docList.add(
                        DocItem(
                            it.name,
                            DocumentsContract.Document.MIME_TYPE_DIR,
                            it.length(),
                            Date(it.lastModified()),
                            Uri.fromFile(it)
                        )
                    )
            } else if (it.name.endsWith(".txt", true)
                || it.name.endsWith(".epub", true)
            ) {
                docList.add(
                    DocItem(
                        it.name,
                        it.extension,
                        it.length(),
                        Date(it.lastModified()),
                        Uri.fromFile(it)
                    )
                )
            }
        }
        docList.sortWith(compareBy({ !it.isDir }, { it.name }))
        adapter.setItems(docList)
    }

    /**
     * 扫描当前文件夹
     */
    private fun scanFolder() {
        rootDoc?.let { doc ->
            adapter.clearItems()
            val lastDoc = subDocs.lastOrNull() ?: doc
            binding.refreshProgressBar.isAutoLoading = true
            launch(IO) {
                viewModel.scanDoc(lastDoc, true, find) {
                    launch {
                        binding.refreshProgressBar.isAutoLoading = false
                    }
                }
            }
        } ?: let {
            val lastPath = AppConfig.importBookPath
            if (lastPath.isNullOrEmpty()) {
                toastOnUi(R.string.empty_msg_import_book)
            } else {
                adapter.clearItems()
                val file = File(path)
                binding.refreshProgressBar.isAutoLoading = true
                launch(IO) {
                    viewModel.scanFile(file, true, find) {
                        launch {
                            binding.refreshProgressBar.isAutoLoading = false
                        }
                    }
                }
            }
        }
    }

    private val find: (docItem: DocItem) -> Unit = {
        launch {
            adapter.addItem(it)
        }
    }

    @Synchronized
    override fun nextDoc(uri: Uri) {
        if (uri.toString().isContentScheme()) {
            subDocs.add(DocumentFile.fromSingleUri(this, uri)!!)
        } else {
            path = uri.path.toString()
        }
        upPath()
    }

    @Synchronized
    private fun goBackDir(): Boolean {
        if (rootDoc == null) {
            if (path != sdPath) {
                File(path).parent?.let {
                    path = it
                    upPath()
                    return true
                }
            }
            return false
        }
        return if (subDocs.isNotEmpty()) {
            subDocs.removeAt(subDocs.lastIndex)
            upPath()
            true
        } else {
            false
        }
    }

    override fun onBackPressed() {
        if (!goBackDir()) {
            super.onBackPressed()
        }
    }

    override fun upCountView() {
        binding.selectActionBar.upCountView(adapter.selectedUris.size, adapter.checkableCount)
    }

}