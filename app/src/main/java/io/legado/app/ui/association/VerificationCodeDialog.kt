package io.legado.app.ui.association

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogVerificationCodeViewBinding
import io.legado.app.help.CacheManager
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.help.source.SourceVerificationHelp
import io.legado.app.lib.theme.primaryColor
import io.legado.app.model.ImageProvider
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

    private var sourceOrigin: String? = null
    private var key = ""

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?): Unit = binding.run {
        initMenu()
        val arguments = arguments ?: return@run
        toolBar.setBackgroundColor(primaryColor)
        toolBar.subtitle = arguments.getString("sourceName")
        sourceOrigin = arguments.getString("sourceOrigin")
        key = SourceVerificationHelp.getKey(sourceOrigin!!)
        val imageUrl = arguments.getString("imageUrl") ?: return@run
        loadImage(imageUrl, sourceOrigin)
        verificationCodeImageView.setOnClickListener {
            showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.verification_code)
        binding.toolBar.menu.applyTint(requireContext())
    }

    @SuppressLint("CheckResult")
    private fun loadImage(url: String, sourceUrl: String?) {
        ImageProvider.bitmapLruCache.remove(url)
        ImageLoader.loadBitmap(requireContext(), url).apply {
            sourceUrl?.let {
                apply(
                    RequestOptions().set(OkHttpModelLoader.sourceOriginOption, it)
                )
            }
        }.error(R.drawable.image_loading_error)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(object : CustomTarget<Bitmap>() {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    errorDrawable?.toBitmap()?.let {
                        onResourceReady(it, null)
                    }
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    view ?: return
                    val bitmap = resource.copy(resource.config, true)
                    ImageProvider.bitmapLruCache.put(url, bitmap)
                    binding.verificationCodeImageView.setImageBitmap(bitmap)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // do nothing
                }
            })
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ok -> {
                val verificationCode = binding.verificationCode.text.toString()
                CacheManager.putMemory(key, verificationCode)
                dismiss()
            }
        }
        return false
    }

    override fun onDestroy() {
        SourceVerificationHelp.checkResult(key)
        super.onDestroy()
        activity?.finish()
    }

}
