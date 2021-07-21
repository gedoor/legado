package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class OnLineImportActivity : BaseActivity<ActivityTranslucenceBinding>(theme = Theme.Transparent) {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.data?.let {
            when (it.path) {
                "bookSource" -> it.getQueryParameter("src")?.let { url ->
                    importBookSource(url)
                }
                "rssSource" -> it.getQueryParameter("src")?.let { url ->
                    importRssSource(url)
                }
                "replaceRule" -> it.getQueryParameter("src")?.let { url ->
                    startActivity<ImportReplaceRuleActivity> {
                        putExtra("source", url)
                    }
                }
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
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportBookSourceDialog().show(supportFragmentManager, "SourceDialog")
            } else {
                errorDialog(getString(R.string.wrong_format))
            }
        })
        viewModel.importSource(url)
    }

    private fun importRssSource(url: String) {
        val viewModel by viewModels<ImportRssSourceViewModel>()
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportRssSourceDialog().show(supportFragmentManager, "SourceDialog")
            } else {
                errorDialog(getString(R.string.wrong_format))
            }
        })
        viewModel.importSource(url)
    }

    private fun errorDialog(msg: String) {
        alert(getString(R.string.error), msg) {
            okButton { }
            onDismiss {
                finish()
            }
        }.show()
    }

}