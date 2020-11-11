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

    private val actions = linkedMapOf<Int, String>()

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
        actions[-1] = getString(R.string.non_action)
        actions[0] = getString(R.string.menu)
        actions[1] = getString(R.string.next_page)
        actions[2] = getString(R.string.prev_page)
        actions[3] = getString(R.string.next_chapter)
        actions[4] = getString(R.string.previous_chapter)
        initData()
        initViewEvent()
    }

    private fun initData() = with(AppConfig) {
        tv_top_left.text = actions[clickActionTopLeft]
        tv_top_center.text = actions[clickActionTopCenter]
        tv_top_right.text = actions[clickActionTopRight]
        tv_middle_left.text = actions[clickActionMiddleLeft]
        tv_middle_right.text = actions[clickActionMiddleRight]
        tv_bottom_left.text = actions[clickActionBottomLeft]
        tv_bottom_center.text = actions[clickActionBottomCenter]
        tv_bottom_right.text = actions[clickActionBottomRight]
    }

    private fun initViewEvent() {
        iv_close.onClick {
            dismiss()
        }
        tv_top_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopLeft, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_top_center.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopCenter, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_top_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTopRight, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_middle_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMiddleLeft, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_middle_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMiddleRight, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_bottom_left.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomLeft, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_bottom_center.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomCenter, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        tv_bottom_right.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBottomRight, action)
                (it as? TextView)?.text = actions[action]
            }
        }
    }

    private fun selectAction(success: (action: Int) -> Unit) {
        selector(
            getString(R.string.select_action),
            actions.values.toList()
        ) { _, index ->
            success.invoke(actions.keys.toList()[index])
        }
    }

}