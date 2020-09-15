package io.legado.app.ui.association

import android.content.Intent
import android.os.Bundle
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_translucence.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast


class FileAssociationActivity : VMBaseActivity<FileAssociationViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: FileAssociationViewModel
        get() = getViewModel(FileAssociationViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        rotate_loading.show()
        viewModel.errorLiveData.observe(this, {
            rotate_loading.hide()
            toast(it)
            finish()
        })
        viewModel.successLiveData.observe(this, {
            rotate_loading.hide()
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
