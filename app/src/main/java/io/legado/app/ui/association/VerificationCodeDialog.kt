package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogVerificationCodeViewBinding
import io.legado.app.help.CacheManager
import io.legado.app.help.SourceVerificationHelp
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.lib.theme.primaryColor
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.utils.applyTint
import io.legado.app.utils.setLayout
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 图片验证码对话框
 * 结果保存在内存中
 * val key = "${sourceOrigin ?: ""}_verificationResult"
 * CacheManager.get(key)
 */
class VerificationCodeDialog() : BaseDialogFragment(R.layout.dialog_verification_code_view),
    Toolbar.OnMenuItemClickListener {

    constructor(
        imageUrl: String,
        sourceOrigin: String? = null,
        sourceName: String? = null
    ) : this() {
        arguments = Bundle().apply {
            putString("imageUrl", imageUrl)
            putString("sourceOrigin", sourceOrigin)
            putString("sourceName", sourceName)
        }
    }

    val binding by viewBinding(DialogVerificationCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initMenu()
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            arguments?.let { arguments ->
                toolBar.subtitle = arguments.getString("sourceName")
                val sourceOrigin = arguments.getString("sourceOrigin")
                arguments.getString("imageUrl")?.let { imageUrl ->
                    ImageLoader.load(requireContext(), imageUrl).apply {
                        sourceOrigin?.let {
                            apply(
                                RequestOptions().set(OkHttpModelLoader.sourceOriginOption, it)
                            )
                        }
                    }.error(R.drawable.image_loading_error)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(verificationCodeImageView)
                    verificationCodeImageView.setOnClickListener {
                        showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
                    }
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.verification_code)
        binding.toolBar.menu.applyTint(requireContext())
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ok -> {
                val sourceOrigin = arguments?.getString("sourceOrigin")
                val key = "${sourceOrigin}_verificationResult"
                val verificationCode = binding.verificationCode.text.toString()
                verificationCode.let {
                    CacheManager.putMemory(key, it)
                    dismiss()
                }
            }
        }
        return false
    }

    override fun onDestroy() {
        SourceVerificationHelp.checkResult()
        super.onDestroy()
        activity?.finish()
    }

}