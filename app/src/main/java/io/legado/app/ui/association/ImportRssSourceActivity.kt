package io.legado.app.ui.association

import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_translucence.*
import org.jetbrains.anko.toast

class ImportRssSourceActivity : VMBaseActivity<ImportRssSourceViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: ImportRssSourceViewModel
        get() = getViewModel(ImportRssSourceViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        rotate_loading.show()
        viewModel.errorLiveData.observe(this, {
            rotate_loading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            rotate_loading.hide()
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
                    rotate_loading.hide()
                    toast(R.string.wrong_format)
                    finish()
                }
            }
        }
    }

    private fun errorDialog(msg: String) {
        alert(getString(R.string.error), msg) {
            okButton { }
        }.show().setOnDismissListener {
            finish()
        }
    }

    private fun successDialog() {
        ImportRssSourceDialog().show(supportFragmentManager, "SourceDialog")
    }

}