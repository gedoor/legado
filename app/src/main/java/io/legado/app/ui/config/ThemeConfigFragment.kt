package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.upTint


class ThemeConfigFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            "transparentStatusBar" -> {
                LiveEventBus.get().with(Bus.recreate).post("")
                Handler().postDelayed({ activity?.recreate() }, 100)
            }
            "colorPrimary", "colorAccent", "colorBackground" -> {
                if (backgroundIsDark(sharedPreferences)) {
                    activity?.let {
                        AlertDialog.Builder(it)
                            .setTitle("白天背景太暗")
                            .setMessage("将会恢复默认背景？")
                            .setPositiveButton(R.string.ok) { _, _ ->
                                App.INSTANCE.putPrefInt(
                                    "colorBackground",
                                    App.INSTANCE.getCompatColor(R.color.md_grey_100)
                                )
                                upTheme(false)
                            }
                            .setNegativeButton(R.string.cancel) { _, _ -> upTheme(false) }
                            .show().upTint()
                    }
                } else {
                    upTheme(false)
                }
            }
            "colorPrimaryNight", "colorAccentNight", "colorBackgroundNight" -> {
                if (backgroundIsLight(sharedPreferences)) {
                    activity?.let {
                        AlertDialog.Builder(it)
                            .setTitle("夜间背景太亮")
                            .setMessage("将会恢复默认背景？")
                            .setPositiveButton(R.string.ok) { _, _ ->
                                App.INSTANCE.putPrefInt(
                                    "colorBackgroundNight",
                                    App.INSTANCE.getCompatColor(R.color.md_grey_800)
                                )
                                upTheme(true)
                            }
                            .setNegativeButton(R.string.cancel) { _, _ -> upTheme(true) }
                            .show().upTint()
                    }
                } else {
                    upTheme(true)
                }
            }
        }

    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "defaultTheme" -> {
                activity?.let {
                    AlertDialog.Builder(it)
                        .setTitle("恢复默认主题")
                        .setMessage("是否确认恢复？")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            preferenceManager.sharedPreferences.edit()
                                .putInt("colorPrimary", App.INSTANCE.getCompatColor(R.color.md_grey_100))
                                .putInt("colorAccent", App.INSTANCE.getCompatColor(R.color.md_pink_600))
                                .putInt("colorBackground", App.INSTANCE.getCompatColor(R.color.md_grey_100))
                                .putInt("colorPrimaryNight", App.INSTANCE.getCompatColor(R.color.md_grey_800))
                                .putInt("colorAccentNight", App.INSTANCE.getCompatColor(R.color.md_pink_800))
                                .putInt("colorBackgroundNight", App.INSTANCE.getCompatColor(R.color.md_grey_800))
                                .apply()
                            App.INSTANCE.upThemeStore()
                            LiveEventBus.get().with(Bus.recreate).post("")
                            Handler().postDelayed({ activity?.recreate() }, 100)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show().upTint()
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun backgroundIsDark(sharedPreferences: SharedPreferences): Boolean {
        return !ColorUtils.isColorLight(
            sharedPreferences.getInt(
                "colorBackground",
                App.INSTANCE.getCompatColor(R.color.md_grey_100)
            )
        )
    }

    private fun backgroundIsLight(sharedPreferences: SharedPreferences): Boolean {
        return ColorUtils.isColorLight(
            sharedPreferences.getInt(
                "colorBackgroundNight",
                App.INSTANCE.getCompatColor(R.color.md_grey_800)
            )
        )
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (App.INSTANCE.getPrefBoolean("isNightTheme", false) == isNightTheme) {
            App.INSTANCE.upThemeStore()
            LiveEventBus.get().with(Bus.recreate).post("")
            Handler().postDelayed({ activity?.recreate() }, 100)
        }
    }

}