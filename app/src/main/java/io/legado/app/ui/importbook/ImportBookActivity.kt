package io.legado.app.ui.importbook

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


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book) {
    private val requestCodeSelectFolder = 342
    private var rootDoc: DocumentFile? = null
    private val subDirs = arrayListOf<String>()
    private lateinit var importBookAdapter: ImportBookAdapter

    override val viewModel: ImportBookViewModel
        get() = getViewModel(ImportBookViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        upPath()
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
        importBookAdapter = ImportBookAdapter(this)
        recycler_view.adapter = importBookAdapter
    }

    private fun upPath() {
        AppConfig.importBookPath?.let {
            val rootUri = Uri.parse(it)
            rootDoc = DocumentFile.fromTreeUri(this, rootUri)
            subDirs.clear()
            tv_path.text = getPath()
        }
    }

    private fun getPath(): String {
        rootDoc?.let {
            return it.name + File.separator + subDirs.joinToString(File.separator)
        }
        return ""
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

                }
            }
        }
    }

}