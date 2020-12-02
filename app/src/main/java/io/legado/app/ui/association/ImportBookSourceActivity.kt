package io.legado.app.ui.association

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.toast


class ImportBookSourceActivity :
    VMBaseActivity<ActivityTranslucenceBinding, ImportBookSourceViewModel>(
        theme = Theme.Transparent
    ) {

    override fun getViewBinding(): ActivityTranslucenceBinding {
        return ActivityTranslucenceBinding.inflate(layoutInflater)
    }

    override val viewModel: ImportBookSourceViewModel
        get() = getViewModel(ImportBookSourceViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                successDialog()
            } else {
                errorDialog(getString(R.string.wrong_format))
            }
        })
        initData()
    }

    private fun initData() {
        intent.getStringExtra("dataKey")?.let {
            IntentDataHelp.getData<String>(it)?.let { source ->
                viewModel.importSource(source)
                return
            }
        }
        intent.getStringExtra("source")?.let {
            viewModel.importSource(it)
            return
        }
        intent.getStringExtra("filePath")?.let {
            viewModel.importSourceFromFilePath(it)
            return
        }
        intent.data?.let {
            when (it.path) {
                "/importonline" -> it.getQueryParameter("src")?.let { url ->
                    if (url.startsWith("http", false)) {
                        viewModel.importSource(url)
                    } else {
                        viewModel.importSourceFromFilePath(url)
                    }
                }
                else -> {
                    binding.rotateLoading.hide()
                    toast(R.string.wrong_format)
                    finish()
                }
            }
        }
    }

    private fun errorDialog(msg: String) {
        alert(getString(R.string.error), msg) {
            okButton { }
            onDismiss {
                finish()
            }
        }.show()
    }

    private fun successDialog() {
        ImportBookSourceDialog().show(supportFragmentManager, "SourceDialog")
    }

}