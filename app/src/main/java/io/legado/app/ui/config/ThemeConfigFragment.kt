package io.legado.app.ui.config

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BasePreferenceFragment
import io.legado.app.constant.AppConst
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppConfig
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.ThemeConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.permission.Permissions
import io.legado.app.lib.permission.PermissionsCompat
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.prefs.ColorPreference
import io.legado.app.utils.*
import java.io.File


@Suppress("SameParameterValue")
class ThemeConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val selectLightBg = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it ?: return@registerForActivityResult
        setBgFromUri(it, PreferKey.bgImage) {
            upTheme(false)
        }
    }
    private val selectDarkBg = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it ?: return@registerForActivityResult
        setBgFromUri(it, PreferKey.bgImageN) {
            upTheme(true)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_theme)
        if (Build.VERSION.SDK_INT < 26) {
            preferenceScreen.removePreferenceRecursively(PreferKey.launcherIcon)
        }
        upPreferenceSummary(PreferKey.bgImage, getPrefString(PreferKey.bgImage))
        upPreferenceSummary(PreferKey.bgImageN, getPrefString(PreferKey.bgImageN))
        upPreferenceSummary(PreferKey.barElevation, AppConfig.elevation.toString())
        findPreference<ColorPreference>(PreferKey.cBackground)?.let {
            it.onSaveColor = { color ->
                if (!ColorUtils.isColorLight(color)) {
                    toastOnUi(R.string.day_background_too_dark)
                    true
                } else {
                    false
                }
            }
        }
        findPreference<ColorPreference>(PreferKey.cNBackground)?.let {
            it.onSaveColor = { color ->
                if (ColorUtils.isColorLight(color)) {
                    toastOnUi(R.string.night_background_too_light)
                    true
                } else {
                    false
                }
            }
        }
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
                ThemeConfig.applyDayNight(requireContext())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PreferKey.launcherIcon -> LauncherIconHelp.changeIcon(getPrefString(key))
            PreferKey.transparentStatusBar -> recreateActivities()
            PreferKey.immNavigationBar -> recreateActivities()
            PreferKey.cPrimary,
            PreferKey.cAccent,
            PreferKey.cBackground,
            PreferKey.cBBackground -> {
                upTheme(false)
            }
            PreferKey.cNPrimary,
            PreferKey.cNAccent,
            PreferKey.cNBackground,
            PreferKey.cNBBackground -> {
                upTheme(true)
            }
        }

    }

    @SuppressLint("PrivateResource")
    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (val key = preference?.key) {
            PreferKey.barElevation -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.bar_elevation))
                .setMaxValue(32)
                .setMinValue(0)
                .setValue(AppConfig.elevation)
                .setCustomButton((R.string.btn_default_s)) {
                    AppConfig.elevation = AppConst.sysElevation
                    recreateActivities()
                }
                .show {
                    AppConfig.elevation = it
                    recreateActivities()
                }
            "themeList" -> ThemeListDialog().show(childFragmentManager, "themeList")
            "saveDayTheme", "saveNightTheme" -> saveThemeAlert(key)
            PreferKey.bgImage -> if (getPrefString(PreferKey.bgImage).isNullOrEmpty()) {
                selectLightBg.launch("image/*")
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.bgImage)
                        upTheme(false)
                    } else {
                        selectLightBg.launch("image/*")
                    }
                }
            }
            PreferKey.bgImageN -> if (getPrefString(PreferKey.bgImageN).isNullOrEmpty()) {
                selectDarkBg.launch("image/*")
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.bgImageN)
                        upTheme(true)
                    } else {
                        selectDarkBg.launch("image/*")
                    }
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    @SuppressLint("InflateParams")
    private fun saveThemeAlert(key: String) {
        alert(R.string.theme_name) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let { themeName ->
                    when (key) {
                        "saveDayTheme" -> {
                            ThemeConfig.saveDayTheme(requireContext(), themeName)
                        }
                        "saveNightTheme" -> {
                            ThemeConfig.saveNightTheme(requireContext(), themeName)
                        }
                    }
                }
            }
            noButton()
        }.show()
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (AppConfig.isNightTheme == isNightTheme) {
            listView.post {
                ThemeConfig.applyTheme(requireContext())
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
            PreferKey.barElevation -> preference.summary =
                getString(R.string.bar_elevation_s, value)
            else -> preference.summary = value
        }
    }

    private fun setBgFromUri(uri: Uri, preferenceKey: String, success: () -> Unit) {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc?.name?.let {
                var file = requireContext().externalFilesDir
                file = FileUtils.createFileIfNotExist(file, preferenceKey, it)
                kotlin.runCatching {
                    DocumentUtils.readBytes(requireContext(), doc.uri)
                }.getOrNull()?.let { byteArray ->
                    file.writeBytes(byteArray)
                    putPrefString(preferenceKey, file.absolutePath)
                    upPreferenceSummary(preferenceKey, file.absolutePath)
                    success()
                } ?: toastOnUi("获取文件出错")
            }
        } else {
            PermissionsCompat.Builder(this)
                .addPermissions(
                    Permissions.READ_EXTERNAL_STORAGE,
                    Permissions.WRITE_EXTERNAL_STORAGE
                )
                .rationale(R.string.bg_image_per)
                .onGranted {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        val imgFile = File(path)
                        if (imgFile.exists()) {
                            var file = requireContext().externalFilesDir
                            file = FileUtils.createFileIfNotExist(file, preferenceKey, imgFile.name)
                            file.writeBytes(imgFile.readBytes())
                            putPrefString(preferenceKey, file.absolutePath)
                            upPreferenceSummary(preferenceKey, file.absolutePath)
                            success()
                        }
                    }
                }
                .request()
        }
    }

}