package io.legado.app.ui.config

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.accentColor
import io.legado.app.utils.getPrefString

class WebDavConfigFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_web_dav)
        findPreference<EditTextPreference>("web_dav_url")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }
            bindPreferenceSummaryToValue(it)
        }
        findPreference<EditTextPreference>("web_dav_account")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
            }
            bindPreferenceSummaryToValue(it)
        }
        findPreference<EditTextPreference>("web_dav_password")?.let {
            it.setOnBindEditTextListener { editText ->
                ATH.setTint(editText, requireContext().accentColor)
                editText.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            }
            bindPreferenceSummaryToValue(it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ATH.applyEdgeEffectColor(listView)
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        newValue?.let {
            val stringValue = it.toString()
            if (preference?.key == "web_dav_password") {
                if (stringValue.isBlank()) {
                    preference.summary = getString(R.string.web_dav_pw_s)
                } else {
                    preference.summary = "*".repeat(stringValue.length)
                }
            }
            if (preference is ListPreference) {
                val index = preference.findIndexOfValue(stringValue)
                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) preference.entries[index] else null)
            } else {
                // For all other preferences, set the summary to the value's
                preference?.summary = stringValue
            }
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

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "web_dav_backup" -> Backup.backup()
            "web_dav_restore" -> WebDavHelp.showRestoreDialog(requireContext())
        }
        return super.onPreferenceTreeClick(preference)
    }

}