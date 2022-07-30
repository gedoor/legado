package io.legado.app.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogDirectLinkUploadConfigBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.lib.theme.primaryColor
import io.legado.app.utils.setLayout
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick

class DirectLinkUploadConfig : BaseDialogFragment(R.layout.dialog_direct_link_upload_config) {

    private val binding by viewBinding(DialogDirectLinkUploadConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        DirectLinkUpload.getRule()?.let {
            binding.editUploadUrl.setText(it.uploadUrl)
            binding.editDownloadUrlRule.setText(it.downloadUrlRule)
            binding.editSummary.setText(it.summary)
        }
        binding.tvCancel.onClick {
            dismiss()
        }
        binding.tvFooterLeft.onClick {
            DirectLinkUpload.delConfig()
            dismiss()
        }
        binding.tvOk.onClick {
            val uploadUrl = binding.editUploadUrl.text?.toString()
            val downloadUrlRule = binding.editDownloadUrlRule.text?.toString()
            val summary = binding.editSummary.text?.toString()
            if (uploadUrl.isNullOrBlank()) {
                toastOnUi("上传Url不能为空")
                return@onClick
            }
            if (downloadUrlRule.isNullOrBlank()) {
                toastOnUi("下载Url规则不能为空")
                return@onClick
            }
            DirectLinkUpload.putConfig(uploadUrl, downloadUrlRule, summary)
            dismiss()
        }
    }

}