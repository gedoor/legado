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
import io.legado.app.help.CacheManager
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class VariableDialog() : BaseDialogFragment(R.layout.dialog_variable, true),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogVariableBinding::bind)
    private val viewModel by viewModels<ViewModel>()

    constructor(key: String, comment: String) : this() {
        arguments = Bundle().apply {
            putString("key", key)
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
            viewModel.init(it) {
                binding.tvComment.text = viewModel.comment
                binding.tvVariable.setText(viewModel.mVariable)
            }
        }
        binding.toolBar.inflateMenu(R.menu.save)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> viewModel.save(binding.tvVariable.text?.toString()) {
                dismiss()
            }
        }
        return true
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        var key: String? = null
        var comment: String? = null
        var mVariable: String? = null

        fun init(arguments: Bundle, onFinally: () -> Unit) {
            if (key != null) return
            execute {
                key = arguments.getString("key")
                comment = arguments.getString("comment")
                mVariable = CacheManager.get("sourceVariable_${key}")
            }.onFinally {
                onFinally.invoke()
            }
        }

        fun save(variable: String?, onFinally: () -> Unit) {
            execute {
                if (variable == null) {
                    CacheManager.delete("sourceVariable_${key}")
                } else {
                    CacheManager.put("sourceVariable_${key}", variable)
                }
            }.onFinally {
                onFinally.invoke()
            }
        }

    }

}