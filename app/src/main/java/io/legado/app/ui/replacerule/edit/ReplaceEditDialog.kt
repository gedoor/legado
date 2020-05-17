package io.legado.app.ui.replacerule.edit

import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.Theme
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import io.legado.app.utils.toast
import kotlinx.android.synthetic.main.dialog_replace_edit.*
import org.jetbrains.anko.displayMetrics
import kotlin.math.abs

class ReplaceEditDialog : DialogFragment(),
    Toolbar.OnMenuItemClickListener,
    KeyboardToolPop.CallBack {

    companion object {

        fun show(
            fragmentManager: FragmentManager,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false
        ) {
            val dialog = ReplaceEditDialog()
            val bundle = Bundle()
            bundle.putLong("id", id)
            bundle.putString("pattern", pattern)
            bundle.putBoolean("isRegex", isRegex)
            dialog.arguments = bundle
            dialog.show(fragmentManager, "editReplace")
        }
    }

    private lateinit var viewModel: ReplaceEditViewModel
    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

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
        mSoftKeyboardTool = KeyboardToolPop(requireContext(), AppConst.keyboardToolChars, this)
        view.viewTreeObserver.addOnGlobalLayoutListener(KeyboardOnGlobalChangeListener())
        tool_bar.inflateMenu(R.menu.replace_edit)
        tool_bar.menu.applyTint(requireContext(), Theme.getTheme())
        tool_bar.setOnMenuItemClickListener(this)
        viewModel.replaceRuleData.observe(viewLifecycleOwner, Observer {
            upReplaceView(it)
        })
        arguments?.let {
            viewModel.initData(it)
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_save -> {
                val rule = getReplaceRule();
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

    override fun sendText(text: String) {
        TODO("Not yet implemented")
    }

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) return
            view?.let { view ->
                it.showAtLocation(view, Gravity.BOTTOM, 0, 0)
            }
        }
    }

    private fun closePopupWindow() {
        mSoftKeyboardTool?.dismiss()
    }

    private inner class KeyboardOnGlobalChangeListener : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            activity?.let {
                val rect = Rect()
                // 获取当前页面窗口的显示范围
                dialog?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
                val screenHeight = it.displayMetrics.heightPixels
                val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
                val preShowing = mIsSoftKeyBoardShowing
                if (abs(keyboardHeight) > screenHeight / 5) {
                    mIsSoftKeyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
                    showKeyboardTopPopupWindow()
                } else {
                    mIsSoftKeyBoardShowing = false
                    if (preShowing) {
                        closePopupWindow()
                    }
                }
            }
        }
    }

    interface CallBack {
        fun onReplaceRuleSave()
    }
}
