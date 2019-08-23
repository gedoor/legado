package io.legado.app.ui.readbook.config

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R

class ReadPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_config_read)
    }


}