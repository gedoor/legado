package io.legado.app.ui.association

import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookSource
import io.legado.app.help.IntentDataHelp
import io.legado.app.help.SourceHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_translucence.*
import org.jetbrains.anko.toast

class ImportBookSourceActivity : VMBaseActivity<ImportBookSourceViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: ImportBookSourceViewModel
        get() = getViewModel(ImportBookSourceViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        rotate_loading.show()
        viewModel.errorLiveData.observe(this, Observer {
            rotate_loading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, Observer {
            rotate_loading.hide()
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
                    rotate_loading.hide()
                    toast("格式不对")
                    finish()
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

    private fun successDialog(allSource: ArrayList<BookSource>) {
        alert("解析结果", "共${allSource.size}个书源,是否确认导入?") {
            okButton {
                SourceHelp.insertBookSource(*allSource.toTypedArray())
            }
            noButton {

            }
        }.show().applyTint().setOnDismissListener {
            finish()
        }
    }
}