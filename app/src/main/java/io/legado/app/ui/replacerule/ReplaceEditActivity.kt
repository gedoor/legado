package io.legado.app.ui.replacerule

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.utils.getViewModel

class ReplaceEditActivity :
    VMBaseActivity<ReplaceEditViewModel>(R.layout.activity_replace_edit, false) {
    override val viewModel: ReplaceEditViewModel
        get() = getViewModel(ReplaceEditViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {

    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }


}
