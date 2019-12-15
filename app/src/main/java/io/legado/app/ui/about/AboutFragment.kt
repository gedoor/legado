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
        findPreference<Preference>("check_update")?.summary = getString(R.string.version) + " " + App.INSTANCE.versionName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "contributors" -> openIntent(Intent.ACTION_VIEW, getString(R.string.contributors_url))
            "update_log" -> UpdateLog().show(childFragmentManager, "update_log")
            "check_update" -> openIntent(Intent.ACTION_VIEW, getString(R.string.latest_release_url))
            "mail" -> openIntent(Intent.ACTION_SENDTO, "mailto:kunfei.ge@gmail.com")
            "git" -> openIntent(Intent.ACTION_VIEW, getString(R.string.this_github_url))
            "home_page" -> openIntent(Intent.ACTION_VIEW, getString(R.string.home_page_url))
            "share_app" -> shareText("App Share",getString(R.string.app_share_description))
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

    private fun shareText(title: String, text: String) {
        try {
            val textIntent = Intent(Intent.ACTION_SEND)
            textIntent.type = "text/plain"
            textIntent.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(textIntent, title))
        } catch (e: Exception) {
            toast(R.string.can_not_share)
        }
    }
}