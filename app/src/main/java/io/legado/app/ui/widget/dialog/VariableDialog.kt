package io.legado.app.ui.widget.dialog

import android.app.Application
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.base.BaseViewModel
import io.legado.app.databinding.DialogVariableBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class VariableDialog() : BaseDialogFragment(R.layout.dialog_variable, true),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogVariableBinding::bind)
    private val viewModel by viewModels<ViewModel>()

    constructor(title: String, key: String, variable: String?, comment: String) : this() {
        arguments = Bundle().apply {
            putString("title", title)
            putString("key", key)
            putString("variable", variable)
            putString("comment", comment)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        arguments?.let {
            binding.toolBar.title = it.getString("title")
            viewModel.init(it) {
                binding.tvComment.text = viewModel.comment
                binding.tvVariable.setText(viewModel.variable)
            }
        } ?: let {
            dismiss()
            return
        }
        binding.toolBar.inflateMenu(R.menu.save)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                callback?.setVariable(
                    viewModel.key ?: "",
                    binding.tvVariable.text?.toString()
                )
                dismissAllowingStateLoss()
            }
        }
        return true
    }

    val callback get() = (parentFragment as? Callback) ?: (activity as? Callback)

    class ViewModel(application: Application) : BaseViewModel(application) {

        var key: String? = null
        var comment: String? = null
        var variable: String? = null

        fun init(arguments: Bundle, onFinally: () -> Unit) {
            if (key != null) return
            execute {
                key = arguments.getString("key")
                comment = arguments.getString("comment")
                variable = arguments.getString("variable")
            }.onFinally {
                onFinally.invoke()
            }
        }

    }

    interface Callback {

        fun setVariable(key: String, variable: String?)

    }

}