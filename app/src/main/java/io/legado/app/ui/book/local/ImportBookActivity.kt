package io.legado.app.ui.book.local

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.filechooser.FilePicker
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.activity_import_book.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File
import java.util.*


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book),
    FileChooserDialog.CallBack,
    PopupMenu.OnMenuItemClickListener,
    ImportBookAdapter.CallBack {
    private val requestCodeSelectFolder = 342
    private var rootDoc: DocumentFile? = null
    private val subDocs = arrayListOf<DocumentFile>()
    private lateinit var adapter: ImportBookAdapter
    private var localUriLiveData: LiveData<List<String>>? = null
    private var sdPath = FileUtils.getSdCardPath()
    private var path = sdPath

    override val viewModel: ImportBookViewModel
        get() = getViewModel(ImportBookViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initEvent()
        initData()
        upRootDoc()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.import_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    private fun initView() {
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ImportBookAdapter(this, this)
        recycler_view.adapter = adapter
        select_action_bar.setMainActionText(R.string.add_to_shelf)
        select_action_bar.inflateMenu(R.menu.import_book_sel)
        select_action_bar.setOnMenuItemClickListener(this)
        select_action_bar.setCallBack(object : SelectActionBar.CallBack {
            override fun selectAll(selectAll: Boolean) {
                adapter.selectAll(selectAll)
            }

            override fun revertSelection() {
                adapter.revertSelection()
            }

            override fun onClickMainAction() {
                viewModel.addToBookshelf(adapter.selectedUris) {
                    upPath()
                }
            }
        })

    }

    private fun initEvent() {
        tv_go_back.onClick {
            goBackDir()
        }
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_folder -> FilePicker.selectFolder(this, requestCodeSelectFolder)
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_del_selection ->
                viewModel.deleteDoc(adapter.selectedUris) {
                    upPath()
                }
        }
        return false
    }

    private fun initData() {
        localUriLiveData?.removeObservers(this)
        localUriLiveData = App.db.bookDao().observeLocalUri()
        localUriLiveData?.observe(this, Observer {
            adapter.upBookHas(it)
        })
    }

    private fun upRootDoc() {
        AppConfig.importBookPath?.let {
            if (it.isContentPath()) {
                val rootUri = Uri.parse(it)
                rootDoc = DocumentFile.fromTreeUri(this, rootUri)
                subDocs.clear()
            } else {
                rootDoc = null
                subDocs.clear()
                path = it
            }
        } ?: let {
            // 没有权限就显示一个授权提示和按钮
            if (PermissionsCompat.check(this, *Permissions.Group.STORAGE)) {
                hint_per.visibility = View.GONE
            } else {
                hint_per.visibility = View.VISIBLE
                tv_request_per.onClick {
                    PermissionsCompat.Builder(this)
                        .addPermissions(*Permissions.Group.STORAGE)
                        .rationale(R.string.tip_perm_request_storage)
                        .onGranted {
                            hint_per.visibility = View.GONE
                            initData()
                            upRootDoc()
                        }
                        .request()
                }
            }
        }
        upPath()
    }

    @SuppressLint("SetTextI18n")
    @Synchronized
    private fun upPath() {
        rootDoc?.let { rootDoc ->
            var path = rootDoc.name.toString() + File.separator
            var lastDoc = rootDoc
            for (doc in subDocs) {
                lastDoc = doc
                path = path + doc.name + File.separator
            }
            tv_path.text = path
            adapter.selectedUris.clear()
            adapter.clearItems()
            launch(IO) {
                val docList = DocumentUtils.listFiles(
                    this@ImportBookActivity,
                    lastDoc.uri
                )
                for (i in docList.lastIndex downTo 0) {
                    val item = docList[i]
                    if (item.name.startsWith(".")) {
                        docList.removeAt(i)
                    } else if (!item.isDir && !item.name.endsWith(".txt", true)) {
                        docList.removeAt(i)
                    }
                }
                docList.sortWith(compareBy({ !it.isDir }, { it.name }))
                withContext(Main) {
                    adapter.setData(docList)
                }
            }
        } ?: let {
            tv_path.text = path.replace(sdPath, "SD")
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
                                Uri.parse(it.absolutePath)
                            )
                        )
                } else if (it.name.endsWith(".txt", true)) {
                    docList.add(
                        DocItem(
                            it.name,
                            it.extension,
                            it.length(),
                            Date(it.lastModified()),
                            Uri.parse(it.absolutePath)
                        )
                    )
                }
            }
            docList.sortWith(compareBy({ !it.isDir }, { it.name }))
            adapter.setData(docList)
        }
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        when (requestCode) {
            requestCodeSelectFolder -> {
                AppConfig.importBookPath = currentPath
                upRootDoc()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestCodeSelectFolder -> if (resultCode == Activity.RESULT_OK) {
                data?.data?.let {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    AppConfig.importBookPath = it.toString()
                    upRootDoc()
                }
            }
        }
    }

    @Synchronized
    override fun nextDoc(uri: Uri) {
        if (uri.toString().isContentPath()) {
            subDocs.add(DocumentFile.fromSingleUri(this, uri)!!)
        } else {
            path = uri.toString()
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
        select_action_bar.upCountView(adapter.selectedUris.size, adapter.checkableCount)
    }

}