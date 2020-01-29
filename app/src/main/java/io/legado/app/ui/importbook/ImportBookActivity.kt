package io.legado.app.ui.importbook

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel


class ImportBookActivity : VMBaseActivity<ImportBookViewModel>(R.layout.activity_import_book) {
    private val requestCodeSelectFolder = 342

    override val viewModel: ImportBookViewModel
        get() = getViewModel(ImportBookViewModel::class.java)


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.import_book, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_folder -> {
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestCodeSelectFolder -> if (resultCode == Activity.RESULT_OK) {

            }
        }
    }

}