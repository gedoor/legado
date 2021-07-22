package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.help.IntentDataHelp
import io.legado.app.lib.dialogs.alert
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ImportReplaceRuleActivity :
    VMBaseActivity<ActivityTranslucenceBinding, ImportReplaceRuleViewModel>(
        theme = Theme.Transparent
    ) {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)
    override val viewModel by viewModels<ImportReplaceRuleViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            errorDialog(it)
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            if (it > 0) {
                ImportReplaceRuleDialog().show(supportFragmentManager, "importReplaceRule")
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
        finish()
    }

    private fun errorDialog(msg: String) {
        alert("导入出错", msg) {
            okButton { }
            onDismiss {
                finish()
            }
        }.show()
    }

}