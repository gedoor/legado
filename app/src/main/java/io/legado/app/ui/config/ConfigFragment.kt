package io.legado.app.ui.config

import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.ui.filechooser.FileChooserDialog
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefBoolean


class ConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val packageManager = App.INSTANCE.packageManager
    private val componentName = ComponentName(
        App.INSTANCE,
        SharedReceiverActivity::class.java.name
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        putPrefBoolean("process_text", isProcessTextEnabled())
        addPreferencesFromResource(R.xml.pref_config)
        bindPreferenceSummaryToValue(findPreference("downloadPath"))
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
            "downloadPath" -> fragmentManager?.let {
                FileChooserDialog.show(
                    it,
                    mode = FileChooserDialog.DIRECTORY
                )
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "downloadPath" -> BookHelp.upDownloadPath()
            "recordLog" -> LogUtils.upLevel()
            "process_text" -> sharedPreferences?.let {
                setProcessTextEnable(it.getBoolean("process_text", true))
            }
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val stringValue = newValue.toString()

        if (preference is ListPreference) {
            val index = preference.findIndexOfValue(stringValue)
            // Set the summary to reflect the new value.
            preference.setSummary(if (index >= 0) preference.entries[index] else null)
        } else {
            // For all other preferences, set the summary to the value's
            preference?.summary = stringValue
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

    private fun getPreferenceString(key: String): String {
        return when (key) {
            "downloadPath" -> getPrefString("downloadPath")
                ?: App.INSTANCE.getExternalFilesDir(null)?.absolutePath
                ?: App.INSTANCE.cacheDir.absolutePath
            else -> getPrefString(key, "")
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
}