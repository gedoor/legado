package io.legado.app.ui.book.search

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogSearchScopeBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class SearchScopeDialog : BaseDialogFragment(R.layout.dialog_search_scope) {

    private val binding by viewBinding(DialogSearchScopeBinding::bind)
    val callback: Callback
        get() {
            return parentFragment as? Callback ?: activity as Callback
        }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        initOtherView()
    }


    private fun initOtherView() {
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
        binding.tvAllSource.setOnClickListener {
            callback.onSearchScopeOk(SearchScope(""))
        }
        binding.tvOk.setOnClickListener {
            callback.onSearchScopeOk(SearchScope(""))
        }
    }


    interface Callback {

        /**
         * 搜索范围确认
         */
        fun onSearchScopeOk(searchScope: SearchScope)

    }

}