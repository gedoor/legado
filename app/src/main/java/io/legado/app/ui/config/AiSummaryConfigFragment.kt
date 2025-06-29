package io.legado.app.ui.config

import android.os.Bundle
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.lib.prefs.PathPreference

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
}