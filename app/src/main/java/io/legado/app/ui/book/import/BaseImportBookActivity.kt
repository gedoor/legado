package io.legado.app.ui.book.import

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.document.HandleFileContract

abstract class BaseImportBookActivity<VB : ViewBinding, VM : ViewModel>(): VMBaseActivity<VB, VM>() {

    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
        }
    }

    /**
     * 设置书籍保存位置
     */
    protected fun setBookStorage() {
        //测试书籍保存位置是否设置
        if (!AppConfig.defaultBookTreeUri.isNullOrBlank()) return
        //测试读写??
        val storageHelp = String(assets.open("storageHelp.md").readBytes())
        val hint = getString(R.string.select_book_folder)
        alert(hint, storageHelp) {
            yesButton {
                localBookTreeSelect.launch {
                    title = hint
                    mode = HandleFileContract.DIR_SYS
                }
            }
            noButton {
                finish()
            }
            onCancelled {
                finish()
            }
        }
    }


}