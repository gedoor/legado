package io.legado.app.ui.config

import android.os.Bundle
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.lib.prefs.PathPreference

import androidx.preference.Preference
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.startActivity

class AiSummaryConfigFragment : PreferenceFragmentCompat() {

    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_ai_summary_config, rootKey)

        openDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            val pathPreference = findPreference<PathPreference>("aiSummaryCachePath")
            pathPreference?.onPathSelected(uri)
        }

        val pathPreference = findPreference<PathPreference>("aiSummaryCachePath")
        pathPreference?.registerResultLauncher(openDirectoryLauncher)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "aiSummaryHelp" -> {
                activity?.startActivity<WebViewActivity> {
                    putExtra("url", "file:///android_asset/web/help/md/AISummaryGuide.md")
                    putExtra("title", getString(R.string.ai_summary_help))
                }
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}