package io.legado.app.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.utils.toast

class AboutFragment : PreferenceFragmentCompat() {

    private val licenseUrl = "https://github.com/gedoor/legado/blob/master/LICENSE"
    private val disclaimerUrl = "https://gedoor.github.io/MyBookshelf/disclaimer.html"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("check_update")?.summary =
            getString(R.string.version) + " " + App.INSTANCE.versionName
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "contributors" -> openIntent(Intent.ACTION_VIEW, R.string.contributors_url)
            "update_log" -> UpdateLog().show(childFragmentManager, "update_log")
            "check_update" -> openIntent(Intent.ACTION_VIEW, R.string.latest_release_url)
            "mail" -> openIntent(Intent.ACTION_SENDTO, "mailto:kunfei.ge@gmail.com")
            "git" -> openIntent(Intent.ACTION_VIEW, R.string.this_github_url)
            "home_page" -> openIntent(Intent.ACTION_VIEW, R.string.home_page_url)
            "license" -> openIntent(Intent.ACTION_VIEW, licenseUrl)
            "disclaimer" -> openIntent(Intent.ACTION_VIEW, disclaimerUrl)
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openIntent(intentName: String, @StringRes addressID: Int) {
        openIntent(intentName, getString(addressID))
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