package io.legado.app.ui.book.read.config

import android.app.Dialog
import android.content.Context
import android.view.KeyEvent
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogPageKeyBinding
import io.legado.app.lib.theme.backgroundColor
import io.legado.app.utils.getPrefString
import io.legado.app.utils.hideSoftInput
import io.legado.app.utils.putPrefString
import org.jetbrains.anko.sdk27.listeners.onClick


class PageKeyDialog(context: Context) : Dialog(context, R.style.AppTheme_AlertDialog) {

    private val binding = DialogPageKeyBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        binding.contentView.setBackgroundColor(context.backgroundColor)
        binding.etPrev.setText(context.getPrefString(PreferKey.prevKeys))
        binding.etNext.setText(context.getPrefString(PreferKey.nextKeys))
        binding.tvOk.onClick {
            context.putPrefString(PreferKey.prevKeys, binding.etPrev.text?.toString())
            context.putPrefString(PreferKey.nextKeys, binding.etNext.text?.toString())
            dismiss()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_DEL) {
            if (binding.etPrev.hasFocus()) {
                val editableText = binding.etPrev.editableText
                if (editableText.isEmpty() or editableText.endsWith(",")) {
                    editableText.append(keyCode.toString())
                } else {
                    editableText.append(",").append(keyCode.toString())
                }
                return true
            } else if (binding.etNext.hasFocus()) {
                val editableText = binding.etNext.editableText
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