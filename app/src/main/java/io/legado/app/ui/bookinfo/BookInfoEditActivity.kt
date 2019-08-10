package io.legado.app.ui.bookinfo

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class BookInfoEditActivity : VMBaseActivity<BookInfoViewModel>(R.layout.activity_book_info_edit) {
    override val viewModel: BookInfoViewModel
        get() = getViewModel(BookInfoViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }
}