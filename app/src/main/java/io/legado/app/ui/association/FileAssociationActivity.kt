package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding

class FileAssociationActivity :
    VMBaseActivity<ActivityTranslucenceBinding, FileAssociationViewModel>(
        theme = Theme.Transparent
    ) {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override val viewModel by viewModels<FileAssociationViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.importBookSourceLive.observe(this) {
            binding.rotateLoading.hide()
            ImportBookSourceDialog.start(supportFragmentManager, it)
        }
        viewModel.importRssSourceLive.observe(this) {
            binding.rotateLoading.hide()
            ImportRssSourceDialog.start(supportFragmentManager, it)
        }
        viewModel.importReplaceRuleLive.observe(this) {
            binding.rotateLoading.hide()
            ImportReplaceRuleDialog.start(supportFragmentManager, it)
        }
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            toastOnUi(it)
            finish()
        })
        viewModel.successLiveData.observe(this, {
            binding.rotateLoading.hide()
            startActivity(it)
            finish()
        })
        intent.data?.let { data ->
            viewModel.dispatchIndent(data)
        }
    }

}
