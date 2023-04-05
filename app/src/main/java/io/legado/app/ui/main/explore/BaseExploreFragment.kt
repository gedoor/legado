package io.legado.app.ui.main.explore

import androidx.appcompat.widget.SearchView
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.lib.theme.primaryTextColor
import io.legado.app.utils.applyTint

abstract class BaseExploreFragment(layoutId: Int) : VMBaseFragment<ExploreViewModel>(layoutId) {

    protected abstract val searchView: SearchView


    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                return false
            }
        })
    }


}