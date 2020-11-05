package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.ReadTipConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.utils.postEvent
import kotlinx.android.synthetic.main.dialog_tip_config.*
import org.jetbrains.anko.sdk27.listeners.onCheckedChange
import org.jetbrains.anko.sdk27.listeners.onClick

class TipConfigDialog : BaseDialogFragment() {

    override fun onStart() {
        super.onStart()
        dialog?.window
            ?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.dialog_tip_config, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initEvent()
    }

    private fun initView() {
        tv_header_left.text = ReadTipConfig.tipHeaderLeftStr
        tv_header_middle.text = ReadTipConfig.tipHeaderMiddleStr
        tv_header_right.text = ReadTipConfig.tipHeaderRightStr
        tv_footer_left.text = ReadTipConfig.tipFooterLeftStr
        tv_footer_middle.text = ReadTipConfig.tipFooterMiddleStr
        tv_footer_right.text = ReadTipConfig.tipFooterRightStr
        sw_hide_header.isChecked = ReadTipConfig.hideHeader
        sw_hide_footer.isChecked = ReadTipConfig.hideFooter
    }

    private fun initEvent() {
        tv_header_left.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderMiddle == i) {
                            tipHeaderMiddle = none
                            tv_header_middle.text = tipArray[none]
                        }
                        if (tipHeaderRight == i) {
                            tipHeaderRight = none
                            tv_header_right.text = tipArray[none]
                        }
                        if (tipFooterLeft == i) {
                            tipFooterLeft = none
                            tv_footer_left.text = tipArray[none]
                        }
                        if (tipFooterMiddle == i) {
                            tipFooterMiddle = none
                            tv_footer_middle.text = tipArray[none]
                        }
                        if (tipFooterRight == i) {
                            tipFooterRight = none
                            tv_footer_right.text = tipArray[none]
                        }
                    }
                    tipHeaderLeft = i
                    tv_header_left.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_header_middle.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderLeft == i) {
                            tipHeaderLeft = none
                            tv_header_left.text = tipArray[none]
                        }
                        if (tipHeaderRight == i) {
                            tipHeaderRight = none
                            tv_header_right.text = tipArray[none]
                        }
                        if (tipFooterLeft == i) {
                            tipFooterLeft = none
                            tv_footer_left.text = tipArray[none]
                        }
                        if (tipFooterMiddle == i) {
                            tipFooterMiddle = none
                            tv_footer_middle.text = tipArray[none]
                        }
                        if (tipFooterRight == i) {
                            tipFooterRight = none
                            tv_footer_right.text = tipArray[none]
                        }
                    }
                    tipHeaderMiddle = i
                    tv_header_middle.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_header_right.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderLeft == i) {
                            tipHeaderLeft = none
                            tv_header_left.text = tipArray[none]
                        }
                        if (tipHeaderMiddle == i) {
                            tipHeaderMiddle = none
                            tv_header_middle.text = tipArray[none]
                        }
                        if (tipFooterLeft == i) {
                            tipFooterLeft = none
                            tv_footer_left.text = tipArray[none]
                        }
                        if (tipFooterMiddle == i) {
                            tipFooterMiddle = none
                            tv_footer_middle.text = tipArray[none]
                        }
                        if (tipFooterRight == i) {
                            tipFooterRight = none
                            tv_footer_right.text = tipArray[none]
                        }
                    }
                    tipHeaderRight = i
                    tv_header_right.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_footer_left.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderLeft == i) {
                            tipHeaderLeft = none
                            tv_header_left.text = tipArray[none]
                        }
                        if (tipHeaderMiddle == i) {
                            tipHeaderMiddle = none
                            tv_header_middle.text = tipArray[none]
                        }
                        if (tipHeaderRight == i) {
                            tipHeaderRight = none
                            tv_header_right.text = tipArray[none]
                        }
                        if (tipFooterMiddle == i) {
                            tipFooterMiddle = none
                            tv_footer_middle.text = tipArray[none]
                        }
                        if (tipFooterRight == i) {
                            tipFooterRight = none
                            tv_footer_right.text = tipArray[none]
                        }
                    }
                    tipFooterLeft = i
                    tv_footer_left.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_footer_middle.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderLeft == i) {
                            tipHeaderLeft = none
                            tv_header_left.text = tipArray[none]
                        }
                        if (tipHeaderMiddle == i) {
                            tipHeaderMiddle = none
                            tv_header_middle.text = tipArray[none]
                        }
                        if (tipHeaderRight == i) {
                            tipHeaderRight = none
                            tv_header_right.text = tipArray[none]
                        }
                        if (tipFooterLeft == i) {
                            tipFooterLeft = none
                            tv_footer_left.text = tipArray[none]
                        }
                        if (tipFooterRight == i) {
                            tipFooterRight = none
                            tv_footer_right.text = tipArray[none]
                        }
                    }
                    tipFooterMiddle = i
                    tv_footer_middle.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        tv_footer_right.onClick {
            selector(items = ReadTipConfig.tipArray.toList()) { _, i ->
                ReadTipConfig.apply {
                    if (i != none) {
                        if (tipHeaderLeft == i) {
                            tipHeaderLeft = none
                            tv_header_left.text = tipArray[none]
                        }
                        if (tipHeaderMiddle == i) {
                            tipHeaderMiddle = none
                            tv_header_middle.text = tipArray[none]
                        }
                        if (tipHeaderRight == i) {
                            tipHeaderRight = none
                            tv_header_right.text = tipArray[none]
                        }
                        if (tipFooterLeft == i) {
                            tipFooterLeft = none
                            tv_footer_left.text = tipArray[none]
                        }
                        if (tipFooterMiddle == i) {
                            tipFooterMiddle = none
                            tv_footer_middle.text = tipArray[none]
                        }
                    }
                    tipFooterRight = i
                    tv_footer_right.text = tipArray[i]
                }
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        sw_hide_header.onCheckedChange { buttonView, isChecked ->
            if (buttonView?.isPressed == true) {
                ReadTipConfig.hideHeader = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        sw_hide_footer.onCheckedChange { buttonView, isChecked ->
            if (buttonView?.isPressed == true) {
                ReadTipConfig.hideFooter = isChecked
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
    }

}