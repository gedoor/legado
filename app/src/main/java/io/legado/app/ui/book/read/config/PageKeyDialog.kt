package io.legado.app.ui.book.read.config

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.getPrefString
import io.legado.app.utils.hideSoftInput
import io.legado.app.utils.putPrefString
import kotlinx.android.synthetic.main.dialog_page_key.*
import org.jetbrains.anko.sdk27.listeners.onClick


class PageKeyDialog(context: Context) : Dialog(context, R.style.AppTheme_AlertDialog) {

    init {
        setContentView(R.layout.dialog_page_key)
        content_view.setBackgroundColor(context.backgroundColor)
        et_prev.setText(context.getPrefString(PreferKey.prevKeys))
        et_next.setText(context.getPrefString(PreferKey.nextKeys))
        tv_ok.onClick {
            context.putPrefString(PreferKey.prevKeys, et_prev.text?.toString())
            context.putPrefString(PreferKey.nextKeys, et_next.text?.toString())
            dismiss()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_DEL) {
            if (et_prev.hasFocus()) {
                val editableText = et_prev.editableText
                if (editableText.isEmpty() or editableText.endsWith(",")) {
                    editableText.append(keyCode.toString())
                } else {
                    editableText.append(",").append(keyCode.toString())
                }
                return true
            } else if (et_next.hasFocus()) {
                val editableText = et_next.editableText
                if (editableText.isEmpty() or editableText.endsWith(",")) {
                    editableText.append(keyCode.toString())
                } else {
                    editableText.append(",").append(keyCode.toString())
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun dismiss() {
        super.dismiss()
        currentFocus?.hideSoftInput()
    }

}