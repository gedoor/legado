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


class ClickActionConfigDialog : BaseDialogFragment() {
    private val binding by viewBinding(DialogClickActionConfigBinding::bind)
    private val actions by lazy {
        linkedMapOf<Int, String>().apply {
            this[-1] = getString(R.string.non_action)
            this[0] = getString(R.string.menu)
            this[1] = getString(R.string.next_page)
            this[2] = getString(R.string.prev_page)
            this[3] = getString(R.string.next_chapter)
            this[4] = getString(R.string.previous_chapter)
            this[5] = getString(R.string.read_aloud_prev_paragraph)
            this[6] = getString(R.string.read_aloud_next_paragraph)
        }
    }

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
        initData()
        initViewEvent()
    }

    private fun initData() = binding.run {
        tvTopLeft.text = actions[AppConfig.clickActionTL]
        tvTopCenter.text = actions[AppConfig.clickActionTC]
        tvTopRight.text = actions[AppConfig.clickActionTR]
        tvMiddleLeft.text = actions[AppConfig.clickActionML]
        tvMiddleRight.text = actions[AppConfig.clickActionMR]
        tvBottomLeft.text = actions[AppConfig.clickActionBL]
        tvBottomCenter.text = actions[AppConfig.clickActionBC]
        tvBottomRight.text = actions[AppConfig.clickActionBR]
    }

    private fun initViewEvent() {
        binding.ivClose.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.tvTopLeft.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTL, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvTopCenter.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTC, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvTopRight.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionTR, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvMiddleLeft.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionML, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvMiddleRight.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionMR, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomLeft.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBL, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomCenter.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBC, action)
                (it as? TextView)?.text = actions[action]
            }
        }
        binding.tvBottomRight.setOnClickListener {
            selectAction { action ->
                putPrefInt(PreferKey.clickActionBR, action)
                (it as? TextView)?.text = actions[action]
            }
        }
    }

    private fun selectAction(success: (action: Int) -> Unit) {
        context?.selector(
            getString(R.string.select_action),
            actions.values.toList()
        ) { _, index ->
            success.invoke(actions.keys.toList()[index])
        }
    }

}