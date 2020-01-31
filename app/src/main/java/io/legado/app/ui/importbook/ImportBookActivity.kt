package io.legado.app.ui.importbook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_import_book.*
import org.jetbrains.anko.sdk27.listeners.onClick
import java.io.File


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book),
    ImportBookAdapter.CallBack {
    private val requestCodeSelectFolder = 342
    private var rootDoc: DocumentFile? = null
    private val subDocs = arrayListOf<DocumentFile>()
    private lateinit var importBookAdapter: ImportBookAdapter
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

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_folder -> selectImportFolder()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        recycler_view.layoutManager = LinearLayoutManager(this)
        importBookAdapter = ImportBookAdapter(this, this)
        recycler_view.adapter = importBookAdapter
    }

    private fun initEvent() {
        tv_go_back.onClick {
            goBackDir()
        }
        btn_add_book.onClick {

        }
        btn_delete.onClick {

        }
    }

    private fun initData() {
        localUriLiveData?.removeObservers(this)
        localUriLiveData = App.db.bookDao().observeLocalUri()
        localUriLiveData?.observe(this, Observer {
            importBookAdapter.upBookHas(it)
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
            val docList = arrayListOf<DocumentFile>()
            lastDoc.listFiles().forEach {
                if (it.isDirectory && it.name?.startsWith(".") == false) {
                    docList.add(it)
                } else if (it.name?.endsWith(".txt", true) == true) {
                    docList.add(it)
                }
            }
            docList.sortWith(compareBy({ !it.isDirectory }, { it.name }))
            tv_path.text = path
            importBookAdapter.selectedUris.clear()
            importBookAdapter.setItems(docList)
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
        if (importBookAdapter.selectedUris.isEmpty()) {
            btn_add_book.setText(R.string.nb_file_add_shelf)
            //设置某些按钮的是否可点击
            setMenuClickable(false)
        } else {
            btn_add_book.text =
                getString(R.string.nb_file_add_shelves, importBookAdapter.selectedUris.size)
            //设置某些按钮的是否可点击
            setMenuClickable(true)
        }
    }

    private fun setMenuClickable(isClickable: Boolean) {
        //设置是否可删除
        btn_delete.isEnabled = isClickable
        btn_delete.isClickable = isClickable
        //设置是否可添加书籍
        btn_add_book.isEnabled = isClickable
        btn_add_book.isClickable = isClickable
    }
}