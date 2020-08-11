package io.legado.app.ui.association

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.Theme
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.getViewModel
import org.jetbrains.anko.toast


class FileAssociationActivity : VMBaseActivity<FileAssociationViewModel>(
    R.layout.activity_translucence,
    theme = Theme.Transparent
) {

    override val viewModel: FileAssociationViewModel
        get() = getViewModel(FileAssociationViewModel::class.java)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.errorLiveData.observe(this, Observer {
            toast(it)
            gotoMainActivity()
        })
        viewModel.successLiveData.observe(this, Observer {
            startActivity(it)
            finish()
        })
        intent.data?.let { data ->
            viewModel.dispatchIndent(data)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //返回后直接跳转到主页面
        gotoMainActivity()
    }

    private fun gotoMainActivity() {
        val mIntent = Intent()
        mIntent.setClass(this, MainActivity::class.java)
        startActivity(mIntent)
    }
}
