package io.legado.app.base

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.legado.app.utils.setIconColor

abstract class BaseFragment(contentLayoutId: Int = 0) : Fragment(contentLayoutId) {

    var supportToolbar: Toolbar? = null
        private set

    val menuInflater: MenuInflater
        get() = SupportMenuInflater(requireContext())


    fun setSupportToolbar(toolbar: Toolbar) {
        supportToolbar = toolbar
        supportToolbar?.let {
            it.menu.apply {
                onCompatCreateOptionsMenu(this)
                setIconColor(requireContext())
            }

            it.setOnMenuItemClickListener { item ->
                onCompatOptionsItemSelected(item)
                true
            }
        }
    }


    open fun onCompatCreateOptionsMenu(menu: Menu) {
    }

    open fun onCompatOptionsItemSelected(item: MenuItem) {
    }

}
