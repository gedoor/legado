package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.utils.*


class ThemeConfigFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyEdgeEffectColor(listView)
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
                recreateActivities()
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
                            .show().applyTint()
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
                            .show().applyTint()
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
                                .putInt("colorPrimary", App.INSTANCE.getCompatColor(R.color.colorPrimary))
                                .putInt("colorAccent", App.INSTANCE.getCompatColor(R.color.colorAccent))
                                .putInt("colorBackground", App.INSTANCE.getCompatColor(R.color.md_grey_100))
                                .putInt("colorPrimaryNight", App.INSTANCE.getCompatColor(R.color.colorPrimary))
                                .putInt("colorAccentNight", App.INSTANCE.getCompatColor(R.color.colorAccent))
                                .putInt("colorBackgroundNight", App.INSTANCE.getCompatColor(R.color.md_grey_800))
                                .apply()
                            App.INSTANCE.upThemeStore()
                            recreateActivities()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show().applyTint()
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
        if (App.INSTANCE.getPrefBoolean("isNightTheme") == isNightTheme) {
            App.INSTANCE.upThemeStore()
            recreateActivities()
        }
    }

    private fun recreateActivities() {
        postEvent(Bus.RECREATE, "")
        Handler().postDelayed({ activity?.recreate() }, 100L)
    }
}