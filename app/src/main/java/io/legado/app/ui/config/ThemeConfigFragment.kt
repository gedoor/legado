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
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
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
                    alert {
                        title = "白天背景太暗"
                        message = "将会恢复默认背景？"
                        yesButton {
                            putPrefInt(
                                "colorBackground",
                                getCompatColor(R.color.md_grey_100)
                            )
                            upTheme(false)
                        }

                        noButton {
                            upTheme(false)
                        }
                    }.show().applyTint()
                } else {
                    upTheme(false)
                }
            }
            "colorPrimaryNight", "colorAccentNight", "colorBackgroundNight" -> {
                if (backgroundIsLight(sharedPreferences)) {
                    alert {
                        title = "夜间背景太亮"
                        message = "将会恢复默认背景？"
                        yesButton {
                            putPrefInt(
                                "colorBackgroundNight",
                                getCompatColor(R.color.md_grey_800)
                            )
                            upTheme(true)
                        }

                        noButton {
                            upTheme(true)
                        }
                    }.show().applyTint()
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
                                .putInt("colorPrimary", getCompatColor(R.color.md_light_blue_500))
                                .putInt("colorAccent", getCompatColor(R.color.md_pink_800))
                                .putInt("colorBackground", getCompatColor(R.color.md_grey_100))
                                .putInt(
                                    "colorPrimaryNight",
                                    getCompatColor(R.color.md_blue_grey_600)
                                )
                                .putInt(
                                    "colorAccentNight",
                                    getCompatColor(R.color.md_deep_orange_800)
                                )
                                .putInt("colorBackgroundNight", getCompatColor(R.color.md_grey_800))
                                .apply()
                            App.INSTANCE.applyTheme()
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
                getCompatColor(R.color.md_grey_100)
            )
        )
    }

    private fun backgroundIsLight(sharedPreferences: SharedPreferences): Boolean {
        return ColorUtils.isColorLight(
            sharedPreferences.getInt(
                "colorBackgroundNight",
                getCompatColor(R.color.md_grey_800)
            )
        )
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (this.isNightTheme == isNightTheme) {
            App.INSTANCE.applyTheme()
            recreateActivities()
        }
    }

    private fun recreateActivities() {
        postEvent(Bus.RECREATE, "")
        Handler().postDelayed({ activity?.recreate() }, 100L)
    }

}