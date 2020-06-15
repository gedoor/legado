package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.preference.Preference
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
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.prefs.IconListPreference
import io.legado.app.utils.*


class ThemeConfigFragment : PreferenceFragmentSupport(), SharedPreferences.OnSharedPreferenceChangeListener {

    val items = arrayListOf("极简", "曜夜", "经典", "黑白", "A屏黑")

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
        if (Build.VERSION.SDK_INT < 26) {
            findPreference<IconListPreference>(PreferKey.launcherIcon)?.let {
                preferenceScreen.removePreference(it)
            }
        }
        upPreferenceSummary("barElevation", AppConfig.elevation.toString())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyEdgeEffectColor(listView)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.theme_config, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_theme_mode -> {
                AppConfig.isNightTheme = !AppConfig.isNightTheme
                App.INSTANCE.applyDayNight()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.launcherIcon -> LauncherIconHelp.changeIcon(getPrefString(key))
            "transparentStatusBar" -> recreateActivities()
            "colorPrimary",
            "colorAccent",
            "colorBackground",
            "colorBottomBackground" -> {
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
            "colorPrimaryNight",
            "colorAccentNight",
            "colorBackgroundNight",
            "colorBottomBackgroundNight" -> {
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

    @SuppressLint("PrivateResource")
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
                            putPrefInt("colorBottomBackground", getCompatColor(R.color.white))
                            AppConfig.isNightTheme = false
                        }
                        4 -> {
                            putPrefInt("colorPrimaryNight", getCompatColor(R.color.black))
                            putPrefInt(
                                "colorAccentNight",
                                getCompatColor(R.color.md_grey_500)
                            )
                            putPrefInt(
                                "colorBackgroundNight",
                                getCompatColor(R.color.black)
                            )
                            putPrefInt("colorBottomBackgroundNight", getCompatColor(R.color.black))
                            AppConfig.isNightTheme = true
                        }
                    }
                    App.INSTANCE.applyDayNight()
                    recreateActivities()
                }
            }.show().applyTint()
            "barElevation" -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.bar_elevation))
                .setMaxValue(32)
                .setMinValue(0)
                .setValue(AppConfig.elevation)
                .setCustomButton((R.string.btn_default_s)) {
                    AppConfig.elevation = App.INSTANCE.resources.getDimension(R.dimen.design_appbar_elevation).toInt()
                    recreateActivities()
                }
                .show {
                    AppConfig.elevation = it
                    recreateActivities()
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
        if (AppConfig.isNightTheme == isNightTheme) {
            listView.post {
                App.INSTANCE.applyTheme()
                recreateActivities()
            }
        }
    }

    private fun recreateActivities() {
        postEvent(EventBus.RECREATE, "")
    }

    private fun upPreferenceSummary(preferenceKey: String, value: String?) {
        val preference = findPreference<Preference>(preferenceKey) ?: return
        when (preferenceKey) {
            "barElevation" -> preference.summary = getString(R.string.bar_elevation_s, value)
        }
    }
}