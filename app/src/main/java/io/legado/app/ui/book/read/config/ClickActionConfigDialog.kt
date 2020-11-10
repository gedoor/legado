package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.getCompatColor
import kotlinx.android.synthetic.main.dialog_click_action_config.*
import org.jetbrains.anko.sdk27.listeners.onClick

class ClickActionConfigDialog : BaseDialogFragment() {

    override fun onStart() {
        super.onStart()
        (activity as ReadBookActivity).bottomDialog++
        dialog?.window?.let {
            it.setBackgroundDrawableResource(R.color.transparent)
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_click_action_config, container)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        view.setBackgroundColor(getCompatColor(R.color.translucent))
        iv_close.onClick {
            dismiss()
        }
    }


}