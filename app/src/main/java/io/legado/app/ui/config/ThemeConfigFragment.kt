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
import io.legado.app.ui.widget.image.CoverImageView
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.prefs.ColorPreference
import io.legado.app.utils.*
import java.io.File


@Suppress("SameParameterValue")
class ThemeConfigFragment : BasePreferenceFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val requestCodeCover = 111
    private val requestCodeCoverDark = 112
    private val requestCodeBgLight = 121
    private val requestCodeBgDark = 122
    private val selectImage = registerForActivityResult(ActivityResultContractUtils.SelectImage()) {
        val uri = it?.second ?: return@registerForActivityResult
        when (it.first) {
            requestCodeCover -> setCoverFromUri(PreferKey.defaultCover, uri)
            requestCodeCoverDark -> setCoverFromUri(PreferKey.defaultCoverDark, uri)
            requestCodeBgLight -> setBgFromUri(uri, PreferKey.bgImage) {
                upTheme(false)
            }
            requestCodeBgDark -> setBgFromUri(uri, PreferKey.bgImageN) {
                upTheme(true)
            }
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
        upPreferenceSummary(PreferKey.defaultCover, getPrefString(PreferKey.defaultCover))
        upPreferenceSummary(PreferKey.defaultCoverDark, getPrefString(PreferKey.defaultCoverDark))
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
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
            PreferKey.defaultCover, PreferKey.defaultCoverDark -> {
                upPreferenceSummary(key, getPrefString(key))
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
                selectImage.launch(requestCodeBgLight)
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.bgImage)
                        upTheme(false)
                    } else {
                        selectImage.launch(requestCodeBgLight)
                    }
                }
            }
            PreferKey.bgImageN -> if (getPrefString(PreferKey.bgImageN).isNullOrEmpty()) {
                selectImage.launch(requestCodeBgDark)
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.bgImageN)
                        upTheme(true)
                    } else {
                        selectImage.launch(requestCodeBgDark)
                    }
                }
            }
            PreferKey.bgImageBlurring -> ImageBlurringDialogFragment(PreferKey.bgImageBlurring) { upTheme(false) }.show(parentFragmentManager)
            PreferKey.bgImageNBlurring -> ImageBlurringDialogFragment(PreferKey.bgImageNBlurring) { upTheme(true) }.show(parentFragmentManager)
            PreferKey.defaultCover -> if (getPrefString(PreferKey.defaultCover).isNullOrEmpty()) {
                selectImage.launch(requestCodeCover)
            } else {
                selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                    if (i == 0) {
                        removePref(PreferKey.defaultCover)
                    } else {
                        selectImage.launch(requestCodeCover)
                    }
                }
            }
            PreferKey.defaultCoverDark ->
                if (getPrefString(PreferKey.defaultCoverDark).isNullOrEmpty()) {
                    selectImage.launch(requestCodeCoverDark)
                } else {
                    selector(items = arrayListOf("删除图片", "选择图片")) { _, i ->
                        if (i == 0) {
                            removePref(PreferKey.defaultCoverDark)
                        } else {
                            selectImage.launch(requestCodeCoverDark)
                        }
                    }
                }
        }
        return super.onPreferenceTreeClick(preference)
    }

    @SuppressLint("InflateParams")
    private fun saveThemeAlert(key: String) {
        alert(R.string.theme_name) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                textInputLayout.hint = "name"
            }
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
            PreferKey.bgImage,
            PreferKey.bgImageN,
            PreferKey.defaultCover,
            PreferKey.defaultCoverDark -> preference.summary = if (value.isNullOrBlank()) {
                getString(R.string.select_image)
            } else {
                value
            }
            else -> preference.summary = value
        }
    }

    private fun setBgFromUri(uri: Uri, preferenceKey: String, success: () -> Unit) {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc?.name?.let {
                var file = requireContext().externalFiles
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
                            var file = requireContext().externalFiles
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

    private fun setCoverFromUri(preferenceKey: String, uri: Uri) {
        if (uri.isContentScheme()) {
            val doc = DocumentFile.fromSingleUri(requireContext(), uri)
            doc?.name?.let {
                var file = requireContext().externalFiles
                file = FileUtils.createFileIfNotExist(file, "covers", it)
                kotlin.runCatching {
                    DocumentUtils.readBytes(requireContext(), doc.uri)
                }.getOrNull()?.let { byteArray ->
                    file.writeBytes(byteArray)
                    putPrefString(preferenceKey, file.absolutePath)
                    CoverImageView.upDefaultCover()
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
                            var file = requireContext().externalFiles
                            file = FileUtils.createFileIfNotExist(file, "covers", imgFile.name)
                            file.writeBytes(imgFile.readBytes())
                            putPrefString(PreferKey.defaultCover, file.absolutePath)
                            CoverImageView.upDefaultCover()
                        }
                    }
                }
                .request()
        }
    }

}