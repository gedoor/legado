package io.legado.app.ui.config

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.prefs.ATEEditTextPreference
import io.legado.app.utils.getPrefString

class WebDavConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_web_dav)
        bindPreferenceSummaryToValue(findPreference("web_dav_url"))
        bindPreferenceSummaryToValue(findPreference("web_dav_account"))
        findPreference<ATEEditTextPreference>("web_dav_password")?.let {
            it.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyEdgeEffectColor(listView)
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
        preference?.apply {
            onPreferenceChangeListener = this@WebDavConfigFragment
            onPreferenceChange(
                this,
                context.getPrefString(key)
            )
        }
    }

}