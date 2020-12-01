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
import io.legado.app.databinding.DialogClickActionConfigBinding
import io.legado.app.help.AppConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.viewbindingdelegate.viewBinding
import org.jetbrains.anko.sdk27.listeners.onClick

class ClickActionConfigDialog : BaseDialogFragment() {
    private val binding by viewBinding(DialogClickActionConfigBinding::bind)
    private val actions = linkedMapOf<Int, String>()

    override fun onStart() {
        super.onStart()
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
        (activity as ReadBookActivity).bottomDialog++
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
        binding.tvTopLeft.text = actions[clickActionTL]
        binding.tvTopCenter.text = actions[clickActionTC]
        binding.tvTopRight.text = actions[clickActionTR]
        binding.tvMiddleLeft.text = actions[clickActionML]
        binding.tvMiddleRight.text = actions[clickActionMR]
        binding.tvBottomLeft.text = actions[clickActionBL]
        binding.tvBottomCenter.text = actions[clickActionBC]
        binding.tvBottomRight.text = actions[clickActionBR]
    }

    private fun initViewEvent() {
        binding.ivClose.onClick {
            dismiss()
        }
        binding.tvTopLeft.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTL, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvTopCenter.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTC, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvTopRight.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTR, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvMiddleLeft.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionML, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvMiddleRight.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMR, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomLeft.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBL, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomCenter.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBC, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomRight.onClick {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBR, action)
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