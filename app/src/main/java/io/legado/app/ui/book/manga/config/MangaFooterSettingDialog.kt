package io.legado.app.ui.book.manga.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.EventBus
import io.legado.app.databinding.DialogMangaFooterSettingBinding
import io.legado.app.help.config.AppConfig
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
            isChecked = config.chapterLabelDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.chapterLabelDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbChapter.run {
            isChecked = config.chapterDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.chapterDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbPageNumberLabel.run {
            isChecked = config.pageNumberLabelDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.pageNumberLabelDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbPageNumber.run {
            isChecked = config.pageNumberDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.pageNumberDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbProgressRatioLabel.run {
            isChecked = config.progressRatioLabelDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.progressRatioLabelDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.cbProgressRatio.run {
            isChecked = config.progressRatioDisable
            setOnCheckedChangeListener { _, isChecked ->
                config.progressRatioDisable = isChecked
                postEvent(EventBus.UP_CONFIG, config)
            }
        }
        binding.rgFooterOrientation.check(if (config.footerOrientation) R.id.rbCenter else R.id.rbLeft)
        binding.rgFooterOrientation.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLeft -> {
                    config.footerOrientation = false
                }

                R.id.rbCenter -> {
                    config.footerOrientation = true
                }
            }
            postEvent(EventBus.UP_CONFIG, config)
        }

        binding.rgFooter.check(if (config.footerDisable) R.id.rbDisable else R.id.rbEnable)
        binding.rgFooter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEnable -> {
                    config.footerDisable = false
                }

                R.id.rbDisable -> {
                    config.footerDisable = true
                }
            }
            postEvent(EventBus.UP_CONFIG, config)
        }
    }

}