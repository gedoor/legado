package io.legado.app.ui.config

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.permission.Permissions
import io.legado.app.help.permission.PermissionsCompat
import io.legado.app.lib.theme.ATH
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.*


class OtherConfigFragment : PreferenceFragmentCompat(),
    FileChooserDialog.CallBack,
    Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val requestCodeDownloadPath = 25324
    private val packageManager = App.INSTANCE.packageManager
    private val componentName = ComponentName(
        App.INSTANCE,
        SharedReceiverActivity::class.java.name
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        putPrefBoolean("process_text", isProcessTextEnabled())
        addPreferencesFromResource(R.xml.pref_config_other)
        bindPreferenceSummaryToValue(findPreference(PreferKey.downloadPath))
        bindPreferenceSummaryToValue(findPreference(PreferKey.threadCount))
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

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            PreferKey.threadCount -> NumberPickerDialog(requireContext())
                .setTitle(getString(R.string.threads_num_title))
                .setMaxValue(999)
                .setMinValue(1)
                .setValue(AppConfig.threadCount)
                .show {
                    requireContext().putPrefInt(PreferKey.threadCount, it)
                    findPreference<Preference>(PreferKey.threadCount)?.summary =
                        getString(R.string.threads_num, it.toString())
                }
            PreferKey.downloadPath -> selectDownloadPathSys()
            PreferKey.cleanCache -> {
                BookHelp.clearCache()
                toast(R.string.clear_cache_success)
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PreferKey.downloadPath -> {
                BookHelp.upDownloadPath()
                findPreference<Preference>(key)?.summary = getPreferenceString(key).toString()
            }
            PreferKey.recordLog -> LogUtils.upLevel()
            PreferKey.processText -> sharedPreferences?.let {
                setProcessTextEnable(it.getBoolean("process_text", true))
            }
            PreferKey.showRss -> postEvent(Bus.SHOW_RSS, "unused")
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val stringValue = newValue.toString()
        when {
            preference is ListPreference -> {
                val index = preference.findIndexOfValue(stringValue)
                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) preference.entries[index] else null)
            }
            preference?.key == PreferKey.threadCount -> preference.summary =
                getString(R.string.threads_num, stringValue)
            else -> preference?.summary = stringValue
        }
        return true
    }

    private fun bindPreferenceSummaryToValue(preference: Preference?) {
        preference?.let {
            preference.onPreferenceChangeListener = this
            onPreferenceChange(
                preference,
                getPreferenceString(preference.key)
            )
        }
    }

    private fun getPreferenceString(key: String): Any {
        return when (key) {
            PreferKey.downloadPath -> getPrefString(PreferKey.downloadPath)
                ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
                ?: App.INSTANCE.cacheDir.absolutePath
            PreferKey.threadCount -> AppConfig.threadCount
            else -> getPrefString(key) ?: ""
        }
    }

    private fun isProcessTextEnabled(): Boolean {
        return packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun setProcessTextEnable(enable: Boolean) {
        if (enable) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun selectDownloadPathSys() {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, requestCodeDownloadPath)
        } catch (e: Exception) {
            selectDownloadPath()
        }
    }

    private fun selectDownloadPath() {
        PermissionsCompat.Builder(this)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                FileChooserDialog.show(
                    childFragmentManager,
                    requestCodeDownloadPath,
                    mode = FileChooserDialog.DIRECTORY,
                    initPath = getPreferenceString(PreferKey.downloadPath).toString()
                )
            }
            .request()
    }

    override fun onFilePicked(requestCode: Int, currentPath: String) {
        if (requestCode == requestCodeDownloadPath) {
            putPrefString(PreferKey.downloadPath, currentPath)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            requestCodeDownloadPath -> if (resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    putPrefString(PreferKey.downloadPath, uri.toString())
                    BookHelp.upDownloadPath()
                }
            }
        }
    }
}