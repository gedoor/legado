package io.legado.app.base

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import io.legado.app.utils.applyTint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseFragment(layoutID: Int) : Fragment(layoutID),
    CoroutineScope by MainScope() {

    var supportToolbar: Toolbar? = null
        private set

    val menuInflater: MenuInflater
        get() = SupportMenuInflater(requireContext())


    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    fun setSupportToolbar(toolbar: Toolbar) {
        supportToolbar = toolbar
        supportToolbar?.let {
            it.menu.apply {
                onCompatCreateOptionsMenu(this)
                applyTint(requireContext())
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
