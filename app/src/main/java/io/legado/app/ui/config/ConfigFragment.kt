package io.legado.app.ui.config

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.apkfuns.logutils.LogUtils
import io.legado.app.App
import io.legado.app.R
import io.legado.app.help.BookHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString


class ConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "downloadPath" -> BookHelp.upDownloadPath()
            "recordLog" -> LogUtils.getLog2FileConfig().configLog2FileEnable(getPrefBoolean("recordLog"))
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

}