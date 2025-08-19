package io.legado.app.ui.config

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.legado.app.R
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.prefs.PathPreference
import io.legado.app.ui.book.read.content.ZhanweifuBookHelp
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi

class AiSummaryConfigFragment : PreferenceFragmentCompat() {

    private lateinit var openDirectoryLauncher: ActivityResultLauncher<Uri?>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_ai_summary_config, rootKey)

        openDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                val pathPreference = findPreference<PathPreference>("aiSummaryCachePath")
                pathPreference?.onPathSelected(uri)
            }

        val pathPreference = findPreference<PathPreference>("aiSummaryCachePath")
        pathPreference?.registerResultLauncher(openDirectoryLauncher)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "aiSummaryClearCache" -> {
                alert(
                    title = getString(R.string.ai_summary_clear_cache),
                    message = getString(R.string.ai_summary_clear_cache_confirm)
                ) {
                    okButton {
                        Coroutine.async {
                            ZhanweifuBookHelp.clearAllAiSummaryCache()
                            toastOnUi(R.string.ai_summary_clear_cache_success)
                        }
                    }
                    cancelButton()
                }.show()
                return true
            }
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
