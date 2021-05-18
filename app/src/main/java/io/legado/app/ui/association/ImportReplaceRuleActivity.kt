package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.toastOnUi

class ImportReplaceRuleActivity :
    VMBaseActivity<ActivityTranslucenceBinding, ImportReplaceRuleViewModel>(
        theme = Theme.Transparent
    ) {

    override fun getViewBinding(): ActivityTranslucenceBinding {
        return ActivityTranslucenceBinding.inflate(layoutInflater)
    }

    override val viewModel: ImportReplaceRuleViewModel
            by viewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
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
        intent.data?.let {
            when (it.path) {
                "/importonline" -> it.getQueryParameter("src")?.let { url ->
                    viewModel.import(url)
                }
                else -> {
                    binding.rotateLoading.hide()
                    toastOnUi("格式不对")
                    finish()
                }
            }
        }
    }

    private fun errorDialog(msg: String) {
        alert("导入出错", msg) {
            okButton { }
            onDismiss {
                finish()
            }
        }.show()
    }

    private fun successDialog(allSource: ArrayList<ReplaceRule>) {
        alert("解析结果", "共${allSource.size}个替换规则,是否确认导入?") {
            okButton {
                appDb.replaceRuleDao.insert(*allSource.toTypedArray())
            }
            noButton()
            onDismiss {
                finish()
            }
        }.show()
    }
}