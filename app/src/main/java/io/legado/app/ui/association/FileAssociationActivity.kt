package io.legado.app.ui.association

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.utils.toastOnUi

class FileAssociationActivity :
    VMBaseActivity<ActivityTranslucenceBinding, FileAssociationViewModel>(
        theme = Theme.Transparent
    ) {

    override fun getViewBinding(): ActivityTranslucenceBinding {
        return ActivityTranslucenceBinding.inflate(layoutInflater)
    }

    override val viewModel: FileAssociationViewModel by viewModels()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
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
