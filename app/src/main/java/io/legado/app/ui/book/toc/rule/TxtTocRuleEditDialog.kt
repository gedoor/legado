package io.legado.app.ui.book.toc.rule

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
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.DialogTocRegexEditBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.GSON
import io.legado.app.utils.applyTint
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class TxtTocRuleEditDialog() : BaseDialogFragment(R.layout.dialog_toc_regex_edit, true),
    Toolbar.OnMenuItemClickListener {

    constructor(id: Long?) : this() {
        id ?: return
        arguments = Bundle().apply {
            putLong("id", id)
        }
    }

    private val binding by viewBinding(DialogTocRegexEditBinding::bind)
    private val viewModel by viewModels<ViewModel>()
    private val callback get() = (parentFragment as? Callback) ?: activity as? Callback

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        initMenu()
        viewModel.initData(arguments?.getLong("id")) {
            binding.tvRuleName.setText(it?.name)
            binding.tvRuleRegex.setText(it?.rule)
            binding.tvRuleExample.setText(it?.example)
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.txt_toc_rule_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                callback?.saveTxtTocRule(getRuleFromView())
                dismissAllowingStateLoss()
            }
            R.id.menu_copy_rule -> context?.sendToClip(GSON.toJson(getRuleFromView()))
            R.id.menu_paste_rule -> {}
        }
        return true
    }

    private fun getRuleFromView(): TxtTocRule {
        val tocRule = viewModel.tocRule ?: TxtTocRule().apply {
            viewModel.tocRule = this
        }
        binding.run {
            tocRule.name = tvRuleName.text.toString()
            tocRule.rule = tvRuleRegex.text.toString()
            tocRule.example = tvRuleExample.text.toString()
        }
        return tocRule
    }

    class ViewModel(application: Application) : BaseViewModel(application) {

        var tocRule: TxtTocRule? = null

        fun initData(id: Long?, finally: (tocRule: TxtTocRule?) -> Unit) {
            execute {
                tocRule?.let {
                    return@execute
                }
                if (id == null) {
                    return@execute
                }
                tocRule = appDb.txtTocRuleDao.get(id)
            }.onFinally {
                finally.invoke(tocRule)
            }
        }

    }

    interface Callback {

        fun saveTxtTocRule(txtTocRule: TxtTocRule)

    }

}