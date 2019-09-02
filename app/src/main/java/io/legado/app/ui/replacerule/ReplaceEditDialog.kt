package io.legado.app.ui.replacerule

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.dialog_replace_edit.*

class ReplaceEditDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener {
    private lateinit var viewModel: ReplaceEditViewModel

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        dialog?.window?.setLayout((dm.widthPixels * 0.9).toInt(), WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(ReplaceEditViewModel::class.java)
        return inflater.inflate(R.layout.dialog_replace_edit, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tool_bar.inflateMenu(R.menu.replace_edit)
        tool_bar.menu.applyTint(requireContext(), false)
        tool_bar.setOnMenuItemClickListener(this)
        viewModel.replaceRuleData.observe(this, Observer {
            upReplaceView(it)
        })
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
            }
        }
        return true
    }

    private fun upReplaceView(replaceRule: ReplaceRule) {
        et_name.setText(replaceRule.name)
        et_group.setText(replaceRule.group)
        et_replace_rule.setText(replaceRule.pattern)
        cb_use_regex.isChecked = replaceRule.isRegex
        et_replace_to.setText(replaceRule.replacement)
        et_scope.setText(replaceRule.scope)
    }

}
