package io.legado.app.ui.config

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogDirectLinkUploadConfigBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.windowSize
import splitties.views.onClick

class DirectLinkUploadConfig : BaseDialogFragment() {

    private val binding by viewBinding(DialogDirectLinkUploadConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        val dm = requireActivity().windowSize
        dialog?.window?.setLayout(
            (dm.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_direct_link_upload_config, container)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.editUploadUrl.setText(DirectLinkUpload.getUploadUrl())
        binding.editDownloadUrlRule.setText(DirectLinkUpload.getDownloadUrlRule())
        binding.tvCancel.onClick {
            dismiss()
        }
        binding.tvFooterLeft.onClick {
            DirectLinkUpload.delete()
        }
        binding.tvOk.onClick {
            val uploadUrl = binding.editUploadUrl.text?.toString()
            val downloadUrlRule = binding.editDownloadUrlRule.text?.toString()
            uploadUrl ?: let {
                toastOnUi("上传Url不能为空")
                return@onClick
            }
            downloadUrlRule ?: let {
                toastOnUi("下载Url规则不能为空")
                return@onClick
            }
            DirectLinkUpload.putUploadUrl(uploadUrl)
            DirectLinkUpload.putDownloadUrlRule(downloadUrlRule)
            dismiss()
        }
    }

}