package io.legado.app.ui.association

import android.content.Intent
import android.os.Bundle
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.databinding.ActivityTranslucenceBinding
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast


class FileAssociationActivity :
    VMBaseActivity<ActivityTranslucenceBinding, FileAssociationViewModel>(
        theme = Theme.Transparent
    ) {

    override fun getViewBinding(): ActivityTranslucenceBinding {
        return ActivityTranslucenceBinding.inflate(layoutInflater)
    }

    override val viewModel: FileAssociationViewModel
        get() = getViewModel(FileAssociationViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.errorLiveData.observe(this, {
            binding.rotateLoading.hide()
            toast(it)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
        //返回后直接跳转到主页面
        gotoMainActivity()
    }

    private fun gotoMainActivity() {
        startActivity<MainActivity>()
    }
}
