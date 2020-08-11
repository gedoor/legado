package io.legado.app.ui.association

import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.RssSource
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.SourceHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.toast

class ImportRssSourceActivity : VMBaseActivity<ImportRssSourceViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: ImportRssSourceViewModel
        get() = getViewModel(ImportRssSourceViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.errorLiveData.observe(this, Observer {
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, Observer {
            successDialog(it)
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
                    toast("格式不对")
                }
            }
        }
    }

    private fun errorDialog(msg: String) {
        alert("导入出错", msg) {
            okButton { }
        }.show().applyTint().setOnDismissListener {
            finish()
        }
    }

    private fun successDialog(allSource: ArrayList<RssSource>) {
        alert("解析结果", "共${allSource.size}个订阅源,是否确认导入?") {
            okButton {
                SourceHelp.insertRssSource(*allSource.toTypedArray())
            }
            noButton {

            }
        }.show().applyTint().setOnDismissListener {
            finish()
        }
    }
}