package io.legado.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.toast

class AboutFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("version")?.summary = App.INSTANCE.versionName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "mail" -> openIntent(Intent.ACTION_SENDTO, "mailto:kunfei.ge@gmail.com")
            "git" -> openIntent(Intent.ACTION_VIEW, getString(R.string.this_github_url))
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun openIntent(intentName: String, address: String) {
        try {
            val intent = Intent(intentName)
            intent.data = Uri.parse(address)
            startActivity(intent)
        } catch (e: Exception) {
            toast(R.string.can_not_open)
        }

    }
}