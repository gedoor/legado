package io.legado.app.ui.book.manga.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.databinding.DialogMangaColorFilterBinding
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MangaColorFilterDialog : BaseDialogFragment(R.layout.dialog_manga_color_filter) {
    private val binding by viewBinding(DialogMangaColorFilterBinding::bind)
    private lateinit var mConfig: MangaColorFilterConfig

    private var mColorFilter: ((MangaColorFilterConfig) -> Unit)? = null

    fun onColorFilter(init: (MangaColorFilterConfig) -> Unit) =
        apply { this.mColorFilter = init }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        mConfig = requireArguments().getParcelable("config") ?: MangaColorFilterConfig()
        binding.seekA.progress = mConfig.a
        binding.seekBrightness.progress = mConfig.l
        binding.seekB.progress = mConfig.b
        binding.seekG.progress = mConfig.g
        binding.seekR.progress = mConfig.r
        binding.seekBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mConfig.l = progress
                mColorFilter?.invoke(mConfig)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.seekR.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mConfig.r = 255 - progress
                mColorFilter?.invoke(mConfig)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.seekG.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mConfig.g = 255 - progress
                mColorFilter?.invoke(mConfig)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.seekB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mConfig.b = 255 - progress
                mColorFilter?.invoke(mConfig)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        binding.seekA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                mConfig.a = 255 - progress
                mColorFilter?.invoke(mConfig)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    companion object {
        fun newInstance(config: MangaColorFilterConfig): MangaColorFilterDialog {
            val args = Bundle()
            args.putParcelable("config", config)
            val fragment = MangaColorFilterDialog()
            fragment.arguments = args
            return fragment
        }
    }
}