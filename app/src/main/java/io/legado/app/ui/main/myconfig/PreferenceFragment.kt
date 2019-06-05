package io.legado.app.ui.main.myconfig

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigViewModel

class PreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)
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
            "isNightTheme" -> {
                App.INSTANCE.initNightTheme()
                App.INSTANCE.upThemeStore()
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        preference?.let {
            when (preference.key) {
                "setting" -> {
                }
                "theme_setting" -> {
                    val intent = Intent(context, ConfigActivity::class.java)
                    intent.putExtra("configType", ConfigViewModel.TYPE_THEME_CONFIG)
                    startActivity(intent)
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

}