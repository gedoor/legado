package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.getSize
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.dialog_read_padding.*

class PaddingConfigDialog : BaseDialogFragment() {

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().getSize()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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
        return inflater.inflate(R.layout.dialog_read_padding, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    private fun initData() = ReadBookConfig.apply {
        //正文
        dsb_padding_top.progress = paddingTop
        dsb_padding_bottom.progress = paddingBottom
        dsb_padding_left.progress = paddingLeft
        dsb_padding_right.progress = paddingRight
        //页眉
        dsb_header_padding_top.progress = headerPaddingTop
        dsb_header_padding_bottom.progress = headerPaddingBottom
        dsb_header_padding_left.progress = headerPaddingLeft
        dsb_header_padding_right.progress = headerPaddingRight
        //页脚
        dsb_footer_padding_top.progress = footerPaddingTop
        dsb_footer_padding_bottom.progress = footerPaddingBottom
        dsb_footer_padding_left.progress = footerPaddingLeft
        dsb_footer_padding_right.progress = footerPaddingRight
        cb_show_top_line.isChecked = showHeaderLine
        cb_show_bottom_line.isChecked = showFooterLine
    }

    private fun initView() = with(ReadBookConfig) {
        //正文
        dsb_padding_top.onChanged = {
            paddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_padding_bottom.onChanged = {
            paddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_padding_left.onChanged = {
            paddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_padding_right.onChanged = {
            paddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页眉
        dsb_header_padding_top.onChanged = {
            headerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_header_padding_bottom.onChanged = {
            headerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_header_padding_left.onChanged = {
            headerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_header_padding_right.onChanged = {
            headerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页脚
        dsb_footer_padding_top.onChanged = {
            footerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_footer_padding_bottom.onChanged = {
            footerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_footer_padding_left.onChanged = {
            footerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_footer_padding_right.onChanged = {
            footerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        cb_show_top_line.onCheckedChangeListener = { cb, isChecked ->
            if (cb.isPressed) {
                showHeaderLine = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        cb_show_bottom_line.onCheckedChangeListener = { cb, isChecked ->
            if (cb.isPressed) {
                showFooterLine = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
    }

}
