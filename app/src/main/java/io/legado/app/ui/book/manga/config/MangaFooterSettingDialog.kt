package io.legado.app.ui.book.manga.config

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
            isChecked = config.disableChapterLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.disableChapterLabel = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbChapter.run {
            isChecked = config.disableChapter
            setOnCheckedChangeListener { _, isChecked ->
                config.disableChapter = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbPageNumberLabel.run {
            isChecked = config.disablePageNumberLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.disablePageNumberLabel = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbPageNumber.run {
            isChecked = config.disablePageNumber
            setOnCheckedChangeListener { _, isChecked ->
                config.disablePageNumber = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbProgressRatioLabel.run {
            isChecked = config.disableProgressRatioLabel
            setOnCheckedChangeListener { _, isChecked ->
                config.disableProgressRatioLabel = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbProgressRatio.run {
            isChecked = config.disableProgressRatio
            setOnCheckedChangeListener { _, isChecked ->
                config.disableProgressRatio = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.rgFooterOrientation.check(if (config.footerOrientation == ReaderInfoBarView.ALIGN_CENTER) R.id.rbCenter else R.id.rbLeft)
        binding.rgFooterOrientation.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLeft -> {
                    config.footerOrientation = ReaderInfoBarView.ALIGN_LEFT
                }

                R.id.rbCenter -> {
                    config.footerOrientation = ReaderInfoBarView.ALIGN_CENTER
                }
            }
            postEvent(EventBus.UP_CONFIG, config)
        }

        binding.rgFooter.check(if (config.disableFooter) R.id.rbDisable else R.id.rbEnable)
        binding.rgFooter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEnable -> {
                    config.disableFooter = false
                }

                R.id.rbDisable -> {
                    config.disableFooter = true
                }
            }
            postEvent(EventBus.UP_CONFIG, config)
        }
    }

}