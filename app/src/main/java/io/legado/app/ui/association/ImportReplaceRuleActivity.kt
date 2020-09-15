package io.legado.app.ui.association

import android.os.Bundle
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.noButton
import io.legado.app.lib.dialogs.okButton
import io.legado.app.utils.applyTint
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_translucence.*
import org.jetbrains.anko.toast

class ImportReplaceRuleActivity : VMBaseActivity<ImportReplaceRuleViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: ImportReplaceRuleViewModel
        get() = getViewModel(ImportReplaceRuleViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        rotate_loading.show()
        viewModel.errorLiveData.observe(this, {
            rotate_loading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            rotate_loading.hide()
            if (it.size > 0) {
                successDialog(it)
            } else {
                errorDialog("格式不对")
            }
        })
        initData()
    }

    private fun initData() {
        intent.getStringExtra("dataKey")?.let {
            IntentDataHelp.getData<String>(it)?.let { source ->
                viewModel.import(source)
                return
            }
        }
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