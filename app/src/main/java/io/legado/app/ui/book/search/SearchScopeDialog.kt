package io.legado.app.ui.book.search

import android.os.Bundle
import android.view.View
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment

class SearchScopeDialog : BaseDialogFragment(R.layout.dialog_search_scope, true) {

    val callback: Callback
        get() {
            return parentFragment as? Callback ?: activity as Callback
        }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {


    }


    interface Callback {

        /**
         * 搜索范围确认
         */
        fun onSearchScopeOk(searchScope: SearchScope)

    }

}