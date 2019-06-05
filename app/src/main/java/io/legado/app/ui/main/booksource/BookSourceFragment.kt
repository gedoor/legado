package io.legado.app.ui.main.booksource

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.setIconColor
import kotlinx.android.synthetic.main.view_titlebar.*

class BookSourceFragment : Fragment(R.layout.fragment_book_source), Toolbar.OnMenuItemClickListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         Log.e("TAG", "BookSourceFragment")
        toolbar.inflateMenu(R.menu.book_source)
        toolbar.menu.setIconColor(App.INSTANCE)
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return false
    }

}