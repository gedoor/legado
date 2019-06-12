package io.legado.app.ui.about

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("version")?.summary = App.INSTANCE.versionName
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {

        }
        return super.onPreferenceTreeClick(preference)
    }
}