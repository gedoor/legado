package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.ReadBookConfig
import io.legado.app.ui.book.read.Help
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.dialog_read_type.*

class ReadTypeDialog : BaseDialogFragment() {

    override fun onStart() {
        super.onStart()
        val dm = DisplayMetrics()
        activity?.let {
            Help.upSystemUiVisibility(it)
            it.windowManager?.defaultDisplay?.getMetrics(dm)
        }
        dialog?.window?.let {
            val attr = it.attributes
            attr.dimAmount = 0.0f
            it.attributes = attr
            it.setLayout((dm.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_read_type, container)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initViewEvent()
        upStyle()
    }

    private fun initView() {
        dsb_text_size.valueFormat = {
            (it + 5).toString()
        }
        dsb_text_letter_spacing.valueFormat = {
            ((it - 50) / 100f).toString()
        }
        dsb_line_size.valueFormat = { ((it - 10) / 10f).toString() }
        dsb_paragraph_spacing.valueFormat = { (it / 10f).toString() }
    }

    private fun initViewEvent() {
        dsb_text_size.onChanged = {
            ReadBookConfig.textSize = it + 5
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_text_letter_spacing.onChanged = {
            ReadBookConfig.letterSpacing = (it - 50) / 100f
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_line_size.onChanged = {
            ReadBookConfig.lineSpacingExtra = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_paragraph_spacing.onChanged = {
            ReadBookConfig.paragraphSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

    private fun upStyle() {
        ReadBookConfig.let {
            dsb_text_size.progress = it.textSize - 5
            dsb_text_letter_spacing.progress = (it.letterSpacing * 100).toInt() + 50
            dsb_line_size.progress = it.lineSpacingExtra
            dsb_paragraph_spacing.progress = it.paragraphSpacing
        }
    }
}