package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogReadPaddingBinding
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.utils.postEvent
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class PaddingConfigDialog : BaseDialogFragment(R.layout.dialog_read_padding) {

    private val binding by viewBinding(DialogReadPaddingBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            it.attributes = attr
        }
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    private fun initData() = binding.run {
        //正文
        dsbPaddingTop.progress = ReadBookConfig.paddingTop
        dsbPaddingBottom.progress = ReadBookConfig.paddingBottom
        dsbPaddingLeft.progress = ReadBookConfig.paddingLeft
        dsbPaddingRight.progress = ReadBookConfig.paddingRight
        //页眉
        dsbHeaderPaddingTop.progress = ReadBookConfig.headerPaddingTop
        dsbHeaderPaddingBottom.progress = ReadBookConfig.headerPaddingBottom
        dsbHeaderPaddingLeft.progress = ReadBookConfig.headerPaddingLeft
        dsbHeaderPaddingRight.progress = ReadBookConfig.headerPaddingRight
        //页脚
        dsbFooterPaddingTop.progress = ReadBookConfig.footerPaddingTop
        dsbFooterPaddingBottom.progress = ReadBookConfig.footerPaddingBottom
        dsbFooterPaddingLeft.progress = ReadBookConfig.footerPaddingLeft
        dsbFooterPaddingRight.progress = ReadBookConfig.footerPaddingRight
        cbShowTopLine.isChecked = ReadBookConfig.showHeaderLine
        cbShowBottomLine.isChecked = ReadBookConfig.showFooterLine
    }

    private fun initView() = binding.run {
        //正文
        dsbPaddingTop.onChanged = {
            ReadBookConfig.paddingTop = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
        }
        dsbPaddingBottom.onChanged = {
            ReadBookConfig.paddingBottom = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
        }
        dsbPaddingLeft.onChanged = {
            ReadBookConfig.paddingLeft = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
        }
        dsbPaddingRight.onChanged = {
            ReadBookConfig.paddingRight = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(10, 5))
        }
        //页眉
        dsbHeaderPaddingTop.onChanged = {
            ReadBookConfig.headerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbHeaderPaddingBottom.onChanged = {
            ReadBookConfig.headerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbHeaderPaddingLeft.onChanged = {
            ReadBookConfig.headerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbHeaderPaddingRight.onChanged = {
            ReadBookConfig.headerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        //页脚
        dsbFooterPaddingTop.onChanged = {
            ReadBookConfig.footerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbFooterPaddingBottom.onChanged = {
            ReadBookConfig.footerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbFooterPaddingLeft.onChanged = {
            ReadBookConfig.footerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        dsbFooterPaddingRight.onChanged = {
            ReadBookConfig.footerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        cbShowTopLine.onCheckedChangeListener = { _, isChecked ->
            ReadBookConfig.showHeaderLine = isChecked
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
        cbShowBottomLine.onCheckedChangeListener = { _, isChecked ->
            ReadBookConfig.showFooterLine = isChecked
            postEvent(EventBus.UP_CONFIG, arrayListOf(2))
        }
    }

}
