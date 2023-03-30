package io.legado.app.ui.document

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.utils.FileDoc

class FileManageViewModel(application: Application) : BaseViewModel(application) {

    val rootDoc = context.getExternalFilesDir(null)?.parentFile?.let {
        FileDoc.fromFile(it)
    }
    val subDocs = arrayListOf<FileDoc>()

}