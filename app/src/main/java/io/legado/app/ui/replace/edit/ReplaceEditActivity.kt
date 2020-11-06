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
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.ui.widget.KeyboardToolPop
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.getViewModel
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.activity_replace_edit.*
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.toast
import kotlin.math.abs

/**
 * 编辑替换规则
 */
class ReplaceEditActivity :
    VMBaseActivity<ReplaceEditViewModel>(R.layout.activity_replace_edit, false),
    ViewTreeObserver.OnGlobalLayoutListener,
    KeyboardToolPop.CallBack {

    companion object {

        fun show(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ) {
            val intent = Intent(context, ReplaceEditActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            context.startActivity(intent)
        }
    }

    override val viewModel: ReplaceEditViewModel
        get() = getViewModel(ReplaceEditViewModel::class.java)

    private var mSoftKeyboardTool: PopupWindow? = null
    private var mIsSoftKeyBoardShowing = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mSoftKeyboardTool = KeyboardToolPop(this, AppConst.keyboardToolChars, this)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(this)
        viewModel.initData(intent) {
            upReplaceView(it)
        }
        iv_help.onClick {
            val mdText = String(assets.open("help/regex.md").readBytes())
            TextDialog.show(supportFragmentManager, mdText, TextDialog.MD)
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
                    toast(R.string.replace_rule_invalid)
                } else {
                    viewModel.save(rule) {
                        postEvent(EventBus.REPLACE_RULE_SAVE, "")
                        finish()
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
        val replaceRule: ReplaceRule = viewModel.replaceRule ?: ReplaceRule()
        replaceRule.name = et_name.text.toString()
        replaceRule.group = et_group.text.toString()
        replaceRule.pattern = et_replace_rule.text.toString()
        replaceRule.isRegex = cb_use_regex.isChecked
        replaceRule.replacement = et_replace_to.text.toString()
        replaceRule.scope = et_scope.text.toString()
        return replaceRule
    }

    private fun insertText(text: String) {
        if (text.isBlank()) return
        val view = window?.decorView?.findFocus()
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
            val view = window?.decorView?.findFocus()
            view?.clearFocus()
        } else {
            insertText(text)
        }
    }

    private fun showKeyboardTopPopupWindow() {
        mSoftKeyboardTool?.let {
            if (it.isShowing) return
            if (!isFinishing) {
                it.showAtLocation(ll_content, Gravity.BOTTOM, 0, 0)
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
        val screenHeight = this.displayMetrics.heightPixels
        val keyboardHeight = screenHeight - rect.bottom // 输入法的高度
        val preShowing = mIsSoftKeyBoardShowing
        if (abs(keyboardHeight) > screenHeight / 5) {
            mIsSoftKeyBoardShowing = true // 超过屏幕五分之一则表示弹出了输入法
            root_view.setPadding(0, 0, 0, 100)
            showKeyboardTopPopupWindow()
        } else {
            mIsSoftKeyBoardShowing = false
            root_view.setPadding(0, 0, 0, 0)
            if (preShowing) {
                closePopupWindow()
            }
        }
    }

}
