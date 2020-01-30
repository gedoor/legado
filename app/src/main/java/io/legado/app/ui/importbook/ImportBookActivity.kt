package io.legado.app.ui.importbook

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.AppConfig
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_import_book.*
import java.io.File


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book),
    ImportBookAdapter.CallBack {
    private val requestCodeSelectFolder = 342
    private var rootDoc: DocumentFile? = null
    private val subDirs = arrayListOf<String>()
    private lateinit var importBookAdapter: ImportBookAdapter

    override val viewModel: ImportBookViewModel
        get() = getViewModel(ImportBookViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
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

    private fun upRootDoc() {
        AppConfig.importBookPath?.let {
            val rootUri = Uri.parse(it)
            rootDoc = DocumentFile.fromTreeUri(this, rootUri)
            subDirs.clear()
            upPath()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun upPath() {
        rootDoc?.let { rootDoc ->
            tv_path.text = rootDoc.name.toString() + File.separator
            var doc: DocumentFile? = rootDoc
            for (dirName in subDirs) {
                doc = doc?.findFile(dirName)
                doc?.let {
                    tv_path.text = tv_path.text.toString() + it.name + File.separator
                }
            }
            doc?.listFiles()?.let {
                importBookAdapter.setItems(it.toList())
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

    override fun findFolder(dirName: String) {
        subDirs.add(dirName)
        upPath()
    }
}