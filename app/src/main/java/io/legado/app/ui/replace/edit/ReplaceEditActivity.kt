package io.legado.app.ui.replace.edit

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.PopupWindow
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ActivityReplaceEditBinding
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.getSize
import io.legado.app.utils.toastOnUi
import kotlin.math.abs

/**
 * 编辑替换规则
 */
class ReplaceEditActivity :
    VMBaseActivity<ActivityReplaceEditBinding, ReplaceEditViewModel>(false),
    ViewTreeObserver.OnGlobalLayoutListener,
    KeyboardToolPop.CallBack {

    companion object {

        fun startIntent(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ): Intent {
            val intent = Intent(context, ReplaceEditActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            return intent
        }

    }

    override fun getViewBinding(): ActivityReplaceEditBinding {
        return ActivityReplaceEditBinding.inflate(layoutInflater)
    }

    override val viewModel: ReplaceEditViewModel
            by viewModels()

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        viewModel.initData(intent) {
            upReplaceView(it)
        }
        binding.ivHelp.setOnClickListener {
            showRegexHelp()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.replace_edit, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> {
                val rule = getReplaceRule()
                if (!rule.isValid()) {
                    toastOnUi(R.string.replace_rule_invalid)
                } else {
                    viewModel.save(rule) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }
        return true
    }

    private fun upReplaceView(replaceRule: ReplaceRule) = with(binding) {
        etName.setText(replaceRule.name)
        etGroup.setText(replaceRule.group)
        etReplaceRule.setText(replaceRule.pattern)
        cbUseRegex.isChecked = replaceRule.isRegex
        etReplaceTo.setText(replaceRule.replacement)
        etScope.setText(replaceRule.scope)
    }

    private fun getReplaceRule(): ReplaceRule = with(binding) {
        val replaceRule: ReplaceRule = viewModel.replaceRule ?: ReplaceRule()
        replaceRule.name = etName.text.toString()
        replaceRule.group = etGroup.text.toString()
        replaceRule.pattern = etReplaceRule.text.toString()
        replaceRule.isRegex = cbUseRegex.isChecked
        replaceRule.replacement = etReplaceTo.text.toString()
        replaceRule.scope = etScope.text.toString()
        return replaceRule
    }

    private fun insertText(text: String) {
        if (text.isBlank()) return
        val view = window?.decorView?.findFocus()
        if (view is EditText) {
            val start = view.selectionStart
            val end = view.selectionEnd
            //TODO 获取EditText的文字
            val edit = view.editableText
            if (start < 0 || start >= edit.length) {
                edit.append(text)
            } else {
                //TODO 光标所在位置插入文字
                edit.replace(start, end, text)
            }
        }
    }

    override fun sendText(text: String) {
        if (text == AppConst.keyboardToolChars[0]) {
            showHelpDialog()
        } else {
            insertText(text)
        }
    }

    private fun showHelpDialog() {
        val items = arrayListOf("正则教程")
        selector(getString(R.string.help), items) { _, index ->
            when (index) {
                0 -> showRegexHelp()
            }
        }
    }

    private fun showRegexHelp() {
        val mdText = String(assets.open("help/regexHelp.md").readBytes())
        TextDialog.show(supportFragmentManager, mdText, TextDialog.MD)
    }

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) return
            if (!isFinishing) {
                it.showAtLocation(binding.llContent, Gravity.BOTTOM, 0, 0)
            }
        }
    }

    private fun closePopupWindow() {
        mSoftKeyboardTool?.dismiss()
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        // 获取当前页面窗口的显示范围
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = this.getSize().heightPixels
        val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
        val preShowing = mIsSoftKeyBoardShowing
        if (abs(keyboardHeight) > screenHeight / 5) {
            mIsSoftKeyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
            binding.rootView.setPadding(0, 0, 0, 100)
            showKeyboardTopPopupWindow()
        } else {
            mIsSoftKeyBoardShowing = false
            binding.rootView.setPadding(0, 0, 0, 0)
            if (preShowing) {
                closePopupWindow()
            }
        }
    }

}
