package io.legado.app.ui.replacerule.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import android.widget.PopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.toast
import kotlinx.android.synthetic.main.dialog_replace_edit.*
import org.jetbrains.anko.sdk27.listeners.onFocusChange

class ReplaceEditDialog : BaseDialogFragment(),
    Toolbar.OnMenuItemClickListener,
    KeyboardToolPop.CallBack {

    companion object {

        fun show(
            fragmentManager: FragmentManager,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ) {
            val dialog = ReplaceEditDialog()
            val bundle = Bundle()
            bundle.putLong("id", id)
            bundle.putString("pattern", pattern)
            bundle.putBoolean("isRegex", isRegex)
            bundle.putString("scope", scope)
            dialog.arguments = bundle
            dialog.show(fragmentManager, this::class.simpleName)
        }
    }

    private lateinit var viewModel: ReplaceEditViewModel
    private lateinit var mSoftKeyboardTool: PopupWindow

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = getViewModel(ReplaceEditViewModel::class.java)
        return inflater.inflate(R.layout.dialog_replace_edit, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        tool_bar.setBackgroundColor(primaryColor)
        mSoftKeyboardTool = KeyboardToolPop(requireContext(), AppConst.keyboardToolChars, this)
        tool_bar.inflateMenu(R.menu.replace_edit)
        tool_bar.menu.applyTint(requireContext())
        tool_bar.setOnMenuItemClickListener(this)
        viewModel.replaceRuleData.observe(viewLifecycleOwner, {
            upReplaceView(it)
        })
        arguments?.let {
            viewModel.initData(it)
        }
        et_replace_rule.onFocusChange { v, hasFocus ->
            if (hasFocus) {
                mSoftKeyboardTool.width = v.width
                mSoftKeyboardTool.showAsDropDown(v)
            } else {
                mSoftKeyboardTool.dismiss()
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                val rule = getReplaceRule()
                if (!rule.isValid()){
                    toast(R.string.replace_rule_invalid)
                }
                else{
                    viewModel.save(rule) {
                        callBack?.onReplaceRuleSave()
                        dismiss()
                    }
                }
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

    private fun getReplaceRule(): ReplaceRule {
        val replaceRule: ReplaceRule = viewModel.replaceRuleData.value ?: ReplaceRule()
        replaceRule.name = et_name.text.toString()
        replaceRule.group = et_group.text.toString()
        replaceRule.pattern = et_replace_rule.text.toString()
        replaceRule.isRegex = cb_use_regex.isChecked
        replaceRule.replacement = et_replace_to.text.toString()
        replaceRule.scope = et_scope.text.toString()
        return replaceRule
    }

    val callBack get() = activity as? CallBack

    private fun insertText(text: String) {
        if (text.isBlank()) return
        val view = dialog?.window?.decorView?.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            val edit = view.editableText//获取EditText的文字
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                edit.replace(start, end, text)//光标所在位置插入文字
            }
        }
    }

    override fun sendText(text: String) {
        if (text == AppConst.keyboardToolChars[0]) {
            val view = dialog?.window?.decorView?.findFocus()
            view?.clearFocus()
        } else {
            insertText(text)
        }
    }

    interface CallBack {
        fun onReplaceRuleSave()
    }
}
