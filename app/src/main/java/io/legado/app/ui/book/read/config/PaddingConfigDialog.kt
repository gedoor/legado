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
import io.legado.app.databinding.DialogReadPaddingBinding
import io.legado.app.help.ReadBookConfig
import io.legado.app.utils.getSize
import io.legado.app.utils.postEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding

class PaddingConfigDialog : BaseDialogFragment() {

    private val binding by viewBinding(DialogReadPaddingBinding::bind)

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
        binding.dsbPaddingTop.progress = paddingTop
        binding.dsbPaddingBottom.progress = paddingBottom
        binding.dsbPaddingLeft.progress = paddingLeft
        binding.dsbPaddingRight.progress = paddingRight
        //页眉
        binding.dsbHeaderPaddingTop.progress = headerPaddingTop
        binding.dsbHeaderPaddingBottom.progress = headerPaddingBottom
        binding.dsbHeaderPaddingLeft.progress = headerPaddingLeft
        binding.dsbHeaderPaddingRight.progress = headerPaddingRight
        //页脚
        binding.dsbFooterPaddingTop.progress = footerPaddingTop
        binding.dsbFooterPaddingBottom.progress = footerPaddingBottom
        binding.dsbFooterPaddingLeft.progress = footerPaddingLeft
        binding.dsbFooterPaddingRight.progress = footerPaddingRight
        binding.cbShowTopLine.isChecked = showHeaderLine
        binding.cbShowBottomLine.isChecked = showFooterLine
    }

    private fun initView() = with(ReadBookConfig) {
        //正文
        binding.dsbPaddingTop.onChanged = {
            paddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbPaddingBottom.onChanged = {
            paddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbPaddingLeft.onChanged = {
            paddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbPaddingRight.onChanged = {
            paddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页眉
        binding.dsbHeaderPaddingTop.onChanged = {
            headerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbHeaderPaddingBottom.onChanged = {
            headerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbHeaderPaddingLeft.onChanged = {
            headerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbHeaderPaddingRight.onChanged = {
            headerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页脚
        binding.dsbFooterPaddingTop.onChanged = {
            footerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbFooterPaddingBottom.onChanged = {
            footerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbFooterPaddingLeft.onChanged = {
            footerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.dsbFooterPaddingRight.onChanged = {
            footerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        binding.cbShowTopLine.onCheckedChangeListener = { cb, isChecked ->
            if (cb.isPressed) {
                showHeaderLine = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        binding.cbShowBottomLine.onCheckedChangeListener = { cb, isChecked ->
            if (cb.isPressed) {
                showFooterLine = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
    }

}
