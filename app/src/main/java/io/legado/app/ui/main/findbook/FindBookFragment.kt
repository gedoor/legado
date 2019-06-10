package io.legado.app.ui.main.findbook

import android.os.Bundle
import android.view.Menu
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseFragment
import kotlinx.android.synthetic.main.view_title_bar.*

class FindBookFragment : BaseFragment(R.layout.fragment_find_book) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(toolbar)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.find_book, menu)
    }

}