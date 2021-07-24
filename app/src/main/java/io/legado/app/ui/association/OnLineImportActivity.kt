package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 网络一键导入
 * 格式: legado://import/{path}?src={url}
 */
class OnLineImportActivity :
    VMBaseActivity<ActivityTranslucenceBinding, OnLineImportViewModel>(theme = Theme.Transparent) {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)
    override val viewModel by viewModels<OnLineImportViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.data?.let {
            val url = it.getQueryParameter("src")
            if (url.isNullOrBlank()) {
                finish()
                return
            }
            when (it.path) {
                "/bookSource" -> ImportBookSourceDialog.start(supportFragmentManager, url, true)
                "/rssSource" -> ImportRssSourceDialog.start(supportFragmentManager, url, true)
                "/replaceRule" -> ImportReplaceRuleDialog.start(supportFragmentManager, url, true)
                "/textTocRule" -> viewModel.importTextTocRule(url, this::finallyDialog)
                "/httpTTS" -> viewModel.importHttpTTS(url, this::finallyDialog)
                "/theme" -> viewModel.importTheme(url, this::finallyDialog)
                "/readConfig" -> viewModel.importReadConfig(url, this::finallyDialog)
                "/importonline" -> when (it.host) {
                    "booksource" -> ImportBookSourceDialog.start(supportFragmentManager, url, true)
                    "rsssource" -> ImportRssSourceDialog.start(supportFragmentManager, url, true)
                    "replace" -> ImportReplaceRuleDialog.start(supportFragmentManager, url, true)
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