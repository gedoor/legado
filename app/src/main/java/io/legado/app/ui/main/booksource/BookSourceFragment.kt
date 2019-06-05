package io.legado.app.ui.main.booksource

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import kotlinx.android.synthetic.main.view_titlebar.*

class BookSourceFragment : BaseFragment(R.layout.fragment_book_source) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.book_source, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {

    }

}