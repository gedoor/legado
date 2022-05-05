package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogVerificationCodeViewBinding
import io.legado.app.help.CacheManager
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 图片验证码对话框
 * 结果保存在数据库中
 * val key = "${sourceOrigin ?: ""}_verificationResult"
 * CacheManager.get(key)
 */
class VerificationCodeDialog() : BaseDialogFragment(R.layout.dialog_verification_code_view) {

    constructor(imageUrl: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("sourceOrigin", sourceOrigin)
            putString("imageUrl", imageUrl)
        }
    }

    val binding by viewBinding(DialogVerificationCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            val sourceOrigin = arguments?.getString("sourceOrigin")
            val key = "${sourceOrigin}_verificationResult"
            arguments?.getString("imageUrl")?.let { imageUrl ->
                ImageLoader.load(requireContext(), imageUrl).apply {
                    sourceOrigin?.let {
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                it
                            )
                        )
                    }
                }.error(R.drawable.image_loading_error)
                    .into(ivImage)
                ivImage.setOnClickListener {
                    showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
                }
            }
            tvOk.setOnClickListener {
                val verificationCode = binding.verificationCode.text.toString()
                verificationCode.let {
                    CacheManager.put(key, it)
                    dismiss()
                }
            }
            tvCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onDestroy() {
        val sourceOrigin = arguments?.getString("sourceOrigin")
        val key = "${sourceOrigin}_verificationResult"
        CacheManager.get(key) ?: CacheManager.put(key, "")
        super.onDestroy()
        activity?.finish()
    }

}