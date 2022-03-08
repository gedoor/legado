package io.legado.app.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogCoverRuleConfigBinding
import io.legado.app.model.BookCover
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick

class CoverRuleConfigDialog : BaseDialogFragment(R.layout.dialog_cover_rule_config) {

    val binding by viewBinding(DialogCoverRuleConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.cbEnable.isChecked = BookCover.coverRuleConfig.enable
        binding.editSearchUrl.setText(BookCover.coverRuleConfig.searchUrl)
        binding.editCoverUrlRule.setText(BookCover.coverRuleConfig.coverRule)
        binding.tvCancel.onClick {
            dismissAllowingStateLoss()
        }
        binding.tvOk.onClick {
            val enable = binding.cbEnable.isChecked
            val searchUrl = binding.editSearchUrl.text?.toString()
            val coverRule = binding.editCoverUrlRule.text?.toString()
            if (searchUrl.isNullOrBlank() || coverRule.isNullOrBlank()) {
                toastOnUi("搜索url和cover规则不能为空")
            } else {
                BookCover.CoverRuleConfig(enable, searchUrl, coverRule).let { config ->
                    BookCover.saveCoverRuleConfig(config)
                }
                dismissAllowingStateLoss()
            }
        }
        binding.tvFooterLeft.onClick {
            BookCover.delCoverRuleConfig()
            dismissAllowingStateLoss()
        }
    }

}