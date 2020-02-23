package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.LauncherIconHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.yesButton
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ColorUtils
import io.legado.app.utils.*
import org.jetbrains.anko.defaultSharedPreferences


class ThemeConfigFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    val items = arrayListOf("极简", "曜夜", "经典", "黑白", "A屏黑")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
        onSharedPreferenceChanged(requireContext().defaultSharedPreferences, "defaultTheme")
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
            PreferKey.launcherIcon -> LauncherIconHelp.changeIcon(getPrefString(key))
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
            "defaultTheme" -> findPreference<Preference>(key)?.summary = items[getPrefInt(key)]
        }

    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "defaultTheme" -> alert(title = "切换默认主题") {
                items(items) { _, which ->
                    when (which) {
                        0 -> {
                            putPrefInt("colorPrimary", getCompatColor(R.color.md_grey_100))
                            putPrefInt("colorAccent", getCompatColor(R.color.lightBlue_color))
                            putPrefInt("colorBackground", getCompatColor(R.color.md_grey_100))
                            AppConfig.isNightTheme = false
                        }
                        1 -> {
                            putPrefInt("colorPrimaryNight", getCompatColor(R.color.shine_color))
                            putPrefInt("colorAccentNight", getCompatColor(R.color.lightBlue_color))
                            putPrefInt("colorBackgroundNight", getCompatColor(R.color.shine_color))
                            AppConfig.isNightTheme = true
                        }
                        2 -> {
                            putPrefInt("colorPrimary", getCompatColor(R.color.md_light_blue_500))
                            putPrefInt("colorAccent", getCompatColor(R.color.md_pink_800))
                            putPrefInt("colorBackground", getCompatColor(R.color.md_grey_100))
                            AppConfig.isNightTheme = false
                        }
                        3 -> {
                            putPrefInt("colorPrimary", getCompatColor(R.color.white))
                            putPrefInt("colorAccent", getCompatColor(R.color.black))
                            putPrefInt("colorBackground", getCompatColor(R.color.white))
                            AppConfig.isNightTheme = false
                        }
                        4 -> {
                            putPrefInt("colorPrimaryNight", getCompatColor(R.color.black))
                            putPrefInt(
                                "colorAccentNight",
                                getCompatColor(R.color.md_grey_600)
                            )
                            putPrefInt(
                                "colorBackgroundNight",
                                getCompatColor(R.color.black)
                            )
                            AppConfig.isNightTheme = true
                        }
                    }
                    putPrefInt("defaultTheme", which)
                    App.INSTANCE.applyDayNight()
                    recreateActivities()
                }
            }.show().applyTint()
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
        if (AppConfig.isNightTheme == isNightTheme) {
            App.INSTANCE.applyTheme()
            recreateActivities()
        }
    }

    private fun recreateActivities() {
        postEvent(EventBus.RECREATE, "")
        Handler().postDelayed({ activity?.recreate() }, 100L)
    }

}