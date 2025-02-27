package io.legado.app.ui.book.manga.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogMangaFooterSettingBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.widget.ReaderInfoBarView
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.postEvent
import io.legado.app.utils.setLayout
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MangaFooterSettingDialog : BaseDialogFragment(R.layout.dialog_manga_footer_setting) {
    val config = GSON.fromJsonObject<MangaFooterConfig>(AppConfig.mangaFooterConfig).getOrNull()
        ?: MangaFooterConfig()
    private val binding by viewBinding(DialogMangaFooterSettingBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.cbChapterLabel.run {
            isChecked = config.hideChapterLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.hideChapterLabel = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbChapter.run {
            isChecked = config.hideChapter
            setOnCheckedChangeListener { _, isChecked ->
                config.hideChapter = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbPageNumberLabel.run {
            isChecked = config.hidePageNumberLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.hidePageNumberLabel = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbPageNumber.run {
            isChecked = config.hidePageNumber
            setOnCheckedChangeListener { _, isChecked ->
                config.hidePageNumber = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbProgressRatioLabel.run {
            isChecked = config.hideProgressRatioLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.hideProgressRatioLabel = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbProgressRatio.run {
            isChecked = config.hideProgressRatio
            setOnCheckedChangeListener { _, isChecked ->
                config.hideProgressRatio = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.cbChapterName.run {
            isChecked = config.hideChapterName
            setOnCheckedChangeListener { _, isChecked ->
                config.hideChapterName = isChecked
                postEvent(EventBus.UP_MANGA_CONFIG, config)
            }
        }
        binding.rgFooterOrientation.check(if (config.footerOrientation == ReaderInfoBarView.ALIGN_CENTER) R.id.rb_center else R.id.rb_left)
        binding.rgFooterOrientation.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_left -> {
                    config.footerOrientation = ReaderInfoBarView.ALIGN_LEFT
                }

                R.id.rb_center -> {
                    config.footerOrientation = ReaderInfoBarView.ALIGN_CENTER
                }
            }
            postEvent(EventBus.UP_MANGA_CONFIG, config)
        }

        binding.rgFooter.check(if (config.hideFooter) R.id.rb_hide else R.id.rb_show)
        binding.rgFooter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_show -> {
                    config.hideFooter = false
                }

                R.id.rb_hide -> {
                    config.hideFooter = true
                }
            }
            postEvent(EventBus.UP_MANGA_CONFIG, config)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        AppConfig.mangaFooterConfig = GSON.toJson(config)
    }
}