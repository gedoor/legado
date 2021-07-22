package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.lib.dialogs.alert
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
                "bookSource" -> importBookSource(url)
                "rssSource" -> importRssSource(url)
                "replaceRule" -> importReplaceRule(url)
                "textTocRule" -> viewModel.importTextTocRule(url, this::finallyDialog)
                "httpTTS" -> viewModel.importHttpTTS(url, this::finallyDialog)
                "theme" -> viewModel.importTheme(url, this::finallyDialog)
                else -> {
                }
            }
        }
    }

    private fun importBookSource(url: String) {
        val viewModel by viewModels<ImportBookSourceViewModel>()
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            finallyDialog(getString(R.string.error), it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportBookSourceDialog().show(supportFragmentManager, "bookSource")
            } else {
                finallyDialog(getString(R.string.error), getString(R.string.wrong_format))
            }
        })
        viewModel.importSource(url)
    }

    private fun importRssSource(url: String) {
        val viewModel by viewModels<ImportRssSourceViewModel>()
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            finallyDialog(getString(R.string.error), it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportRssSourceDialog().show(supportFragmentManager, "rssSource")
            } else {
                finallyDialog(getString(R.string.error), getString(R.string.wrong_format))
            }
        })
        viewModel.importSource(url)
    }

    private fun importReplaceRule(url: String) {
        val viewModel by viewModels<ImportReplaceRuleViewModel>()
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            finallyDialog(getString(R.string.error), it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportReplaceRuleDialog().show(supportFragmentManager, "replaceRule")
            } else {
                finallyDialog(getString(R.string.error), getString(R.string.wrong_format))
            }
        })
        viewModel.import(url)
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