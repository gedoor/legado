package io.legado.app.ui.association

import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.toast

class ImportReplaceRuleActivity : VMBaseActivity<ImportReplaceRuleViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: ImportReplaceRuleViewModel
        get() = getViewModel(ImportReplaceRuleViewModel::class.java)

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
        intent.getStringExtra("source")?.let {
            viewModel.import(it)
            return
        }
        intent.getStringExtra("filePath")?.let {
            viewModel.importFromFilePath(it)
            return
        }
        intent.data?.let {
            when (it.path) {
                "/importonline" -> it.getQueryParameter("src")?.let { url ->
                    if (url.startsWith("http", false)) {
                        viewModel.import(url)
                    } else {
                        viewModel.importFromFilePath(url)
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

    private fun successDialog(allSource: ArrayList<ReplaceRule>) {
        alert("解析结果", "共${allSource.size}个替换规则,是否确认导入?") {
            okButton {
                App.db.replaceRuleDao().insert(*allSource.toTypedArray())
            }
            noButton {

            }
        }.show().applyTint().setOnDismissListener {
            finish()
        }
    }
}