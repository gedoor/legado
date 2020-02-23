package io.legado.app.ui.importbook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.widget.SelectActionBar
import io.legado.app.utils.DocumentUtils
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_import_book.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book),
    PopupMenu.OnMenuItemClickListener,
    ImportBookAdapter.CallBack {
    private val requestCodeSelectFolder = 342
    private var rootDoc: DocumentFile? = null
    private val subDocs = arrayListOf<DocumentFile>()
    private lateinit var adapter: ImportBookAdapter
    private var localUriLiveData: LiveData<List<String>>? = null

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
        recycler_view.layoutManager = LinearLayoutManager(this)
        adapter = ImportBookAdapter(this, this)
        recycler_view.adapter = adapter
        rotate_loading.loadingColor = accentColor
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
            R.id.menu_select_folder -> selectImportFolder()
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
            val rootUri = Uri.parse(it)
            rootDoc = DocumentFile.fromTreeUri(this, rootUri)
            subDocs.clear()
            upPath()
        }
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
            rotate_loading.show()
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
                    rotate_loading.hide()
                    adapter.setData(docList)
                }
            }
        }
    }

    private fun selectImportFolder() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, requestCodeSelectFolder)
        } catch (e: Exception) {
            e.printStackTrace()
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
    override fun nextDoc(doc: DocumentFile) {
        subDocs.add(doc)
        upPath()
    }

    @Synchronized
    private fun goBackDir(): Boolean {
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