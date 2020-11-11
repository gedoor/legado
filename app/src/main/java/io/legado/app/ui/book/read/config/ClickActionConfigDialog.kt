package io.legado.app.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.putPrefInt
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
        initData()
        initViewEvent()
    }

    private fun initData() = with(AppConfig) {
        tv_top_left.text = getActionString(clickActionTopLeft)
        tv_top_center.text = getActionString(clickActionTopCenter)
        tv_top_right.text = getActionString(clickActionTopRight)
        tv_middle_left.text = getActionString(clickActionMiddleLeft)
        tv_middle_right.text = getActionString(clickActionMiddleRight)
        tv_bottom_left.text = getActionString(clickActionBottomLeft)
        tv_bottom_center.text = getActionString(clickActionBottomCenter)
        tv_bottom_right.text = getActionString(clickActionBottomRight)
    }

    private fun getActionString(action: Int): String {
        return when (action) {
            0 -> getString(R.string.menu)
            2 -> getString(R.string.prev_page)
            else -> getString(R.string.next_page)
        }
    }

    private fun initViewEvent() {
        iv_close.onClick {
            dismiss()
        }
        tv_top_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopLeft, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_top_center.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopCenter, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_top_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopRight, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_middle_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMiddleLeft, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_middle_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMiddleRight, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_bottom_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomLeft, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_bottom_center.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomCenter, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
        tv_bottom_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomRight, action)
                (it as? TextView)?.text = getActionString(action)
            }
        }
    }

    private fun selectAction(success: (action: Int) -> Unit) {
        val actions = arrayListOf(
            getString(R.string.menu),
            getString(R.string.next_page),
            getString(R.string.prev_page)
        )
        selector(
            getString(R.string.select_action),
            actions
        ) { _, index ->
            success.invoke(index)
        }
    }

}