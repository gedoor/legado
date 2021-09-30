package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 网络一键导入
 * 格式: legado://import/{path}?src={url}
 */
class OnLineImportActivity :
    VMBaseActivity<ActivityTranslucenceBinding, OnLineImportViewModel>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)
    override val viewModel by viewModels<OnLineImportViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.successLive.observe(this) {
            when (it.first) {
                "bookSource" -> showDialogFragment(
                    ImportBookSourceDialog(it.second, true)
                )
                "rssSource" -> showDialogFragment(
                    ImportRssSourceDialog(it.second, true)
                )
                "replaceRule" -> showDialogFragment(
                    ImportReplaceRuleDialog(it.second, true)
                )
            }
        }
        viewModel.errorLive.observe(this) {
            finallyDialog(getString(R.string.error), it)
        }
        intent.data?.let {
            val url = it.getQueryParameter("src")
            if (url.isNullOrBlank()) {
                finish()
                return
            }
            when (it.path) {
                "/bookSource" -> showDialogFragment(
                    ImportBookSourceDialog(url, true)
                )
                "/rssSource" -> showDialogFragment(
                    ImportRssSourceDialog(url, true)
                )
                "/replaceRule" -> showDialogFragment(
                    ImportReplaceRuleDialog(url, true)
                )
                "/textTocRule" -> viewModel.getText(url) { json ->
                    viewModel.importTextTocRule(json, this::finallyDialog)
                }
                "/httpTTS" -> viewModel.getText(url) { json ->
                    viewModel.importHttpTTS(json, this::finallyDialog)
                }
                "/theme" -> viewModel.getText(url) { json ->
                    viewModel.importTheme(json, this::finallyDialog)
                }
                "/readConfig" -> viewModel.getBytes(url) { bytes ->
                    viewModel.importReadConfig(bytes, this::finallyDialog)
                }
                "/importonline" -> when (it.host) {
                    "booksource" -> showDialogFragment(
                        ImportBookSourceDialog(url, true)
                    )
                    "rsssource" -> showDialogFragment(
                        ImportRssSourceDialog(url, true)
                    )
                    "replace" -> showDialogFragment(
                        ImportReplaceRuleDialog(url, true)
                    )
                    else -> {
                        toastOnUi("url error")
                        finish()
                    }
                }
            }
        }
    }

    private fun finallyDialog(title: String, msg: String) {
        alert(title, msg) {
            okButton()
            onDismiss {
                finish()
            }
        }.show()
    }

}