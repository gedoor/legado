package io.legado.app.ui.book.read.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.help.ReadBookConfig
import io.legado.app.help.ReadTipConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.utils.checkByIndex
import io.legado.app.utils.getIndexById
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
        rg_title_mode.checkByIndex(ReadBookConfig.titleMode)
        dsb_title_size.progress = ReadBookConfig.titleSize
        dsb_title_top.progress = ReadBookConfig.titleTopSpacing
        dsb_title_bottom.progress = ReadBookConfig.titleBottomSpacing

        tv_header_show.text = ReadTipConfig.headerModes[ReadTipConfig.headerMode]
        tv_footer_show.text = ReadTipConfig.footerModes[ReadTipConfig.footerMode]

        tv_header_left.text = ReadTipConfig.tipHeaderLeftStr
        tv_header_middle.text = ReadTipConfig.tipHeaderMiddleStr
        tv_header_right.text = ReadTipConfig.tipHeaderRightStr
        tv_footer_left.text = ReadTipConfig.tipFooterLeftStr
        tv_footer_middle.text = ReadTipConfig.tipFooterMiddleStr
        tv_footer_right.text = ReadTipConfig.tipFooterRightStr
    }

    private fun initEvent() {
        rg_title_mode.onCheckedChange { _, checkedId ->
            ReadBookConfig.titleMode = rg_title_mode.getIndexById(checkedId)
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_title_size.onChanged = {
            ReadBookConfig.titleSize = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_title_top.onChanged = {
            ReadBookConfig.titleTopSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsb_title_bottom.onChanged = {
            ReadBookConfig.titleBottomSpacing = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        ll_header_show.onClick {
            selector(items = ReadTipConfig.headerModes.values.toList()) { _, i ->
                ReadTipConfig.headerMode = ReadTipConfig.headerModes.keys.toList()[i]
                tv_header_show.text = ReadTipConfig.headerModes[ReadTipConfig.headerMode]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_footer_show.onClick {
            selector(items = ReadTipConfig.footerModes.values.toList()) { _, i ->
                ReadTipConfig.footerMode = ReadTipConfig.footerModes.keys.toList()[i]
                tv_footer_show.text = ReadTipConfig.footerModes[ReadTipConfig.footerMode]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_header_left.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipHeaderLeft = i
                tv_header_left.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_header_middle.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipHeaderMiddle = i
                tv_header_middle.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_header_right.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipHeaderRight = i
                tv_header_right.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_footer_left.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipFooterLeft = i
                tv_footer_left.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_footer_middle.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipFooterMiddle = i
                tv_footer_middle.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
        ll_footer_right.onClick {
            selector(items = ReadTipConfig.tips) { _, i ->
                clearRepeat(i)
                ReadTipConfig.tipFooterRight = i
                tv_footer_right.text = ReadTipConfig.tips[i]
                postEvent(EventBus.UP_CONFIG, true)
            }
        }
    }

    private fun clearRepeat(repeat: Int) = ReadTipConfig.apply {
        if (repeat != none) {
            if (tipHeaderLeft == repeat) {
                tipHeaderLeft = none
                tv_header_left.text = tips[none]
            }
            if (tipHeaderMiddle == repeat) {
                tipHeaderMiddle = none
                tv_header_middle.text = tips[none]
            }
            if (tipHeaderRight == repeat) {
                tipHeaderRight = none
                tv_header_right.text = tips[none]
            }
            if (tipFooterLeft == repeat) {
                tipFooterLeft = none
                tv_footer_left.text = tips[none]
            }
            if (tipFooterMiddle == repeat) {
                tipFooterMiddle = none
                tv_footer_middle.text = tips[none]
            }
            if (tipFooterRight == repeat) {
                tipFooterRight = none
                tv_footer_right.text = tips[none]
            }
        }
    }

}