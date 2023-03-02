package io.legado.app.ui.dict.rule

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
import io.legado.app.data.entities.DictRule
import io.legado.app.databinding.DialogDictRuleEditBinding
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.*
import io.legado.app.utils.viewbindingdelegate.viewBinding

class DictRuleEditDialog() : BaseDialogFragment(R.layout.dialog_dict_rule_edit, true),
    Toolbar.OnMenuItemClickListener {

    val viewModel by viewModels<DictRuleEditViewModel>()
    val binding by viewBinding(DialogDictRuleEditBinding::bind)

    constructor(name: String) : this() {
        arguments = Bundle().apply {
            putString("name", name)
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.inflateMenu(R.menu.dict_rule_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        viewModel.initData(arguments?.getString("name")) {
            upRuleView(viewModel.dictRule)
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> viewModel.save(getDictRule()) {
                dismissAllowingStateLoss()
            }
            R.id.menu_copy_rule -> viewModel.copyRule(getDictRule())
            R.id.menu_paste_rule -> viewModel.pasteRule {
                upRuleView(it)
            }
        }
        return true
    }

    private fun upRuleView(dictRule: DictRule?) {
        binding.tvRuleName.setText(dictRule?.name)
        binding.tvUrlRule.setText(dictRule?.urlRule)
        binding.tvShowRule.setText(dictRule?.showRule)
    }

    private fun getDictRule(): DictRule {
        val dictRule = viewModel.dictRule?.copy() ?: DictRule()
        dictRule.name = binding.tvRuleName.text.toString()
        dictRule.urlRule = binding.tvUrlRule.text.toString()
        dictRule.showRule = binding.tvShowRule.text.toString()
        return dictRule
    }

    class DictRuleEditViewModel(application: Application) : BaseViewModel(application) {

        var dictRule: DictRule? = null

        fun initData(name: String?, onFinally: () -> Unit) {
            execute {
                if (dictRule == null && name != null) {
                    dictRule = appDb.dictRuleDao.getByName(name)
                }
            }.onFinally {
                onFinally.invoke()
            }
        }

        fun save(newDictRule: DictRule, onFinally: () -> Unit) {
            execute {
                dictRule?.let {
                    appDb.dictRuleDao.delete(it)
                }
                appDb.dictRuleDao.insert(newDictRule)
                dictRule = newDictRule
            }.onFinally {
                onFinally.invoke()
            }
        }

        fun copyRule(dictRule: DictRule) {
            context.sendToClip(GSON.toJson(dictRule))
        }

        fun pasteRule(success: (DictRule) -> Unit) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                context.toastOnUi("剪贴板没有内容")
                return
            }
            execute {
                GSON.fromJsonObject<DictRule>(text).getOrThrow()
            }.onSuccess {
                success.invoke(it)
            }.onError {
                context.toastOnUi("格式不对")
            }
        }

    }

}