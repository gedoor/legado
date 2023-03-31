package io.legado.app.ui.document

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.utils.toastOnUi
import java.io.File

class FileManageViewModel(application: Application) : BaseViewModel(application) {

    val rootDoc = context.getExternalFilesDir(null)?.parentFile
    var subDocs = mutableListOf<File>()
    val filesLiveData = MutableLiveData<List<File>>()

    fun upFiles(parentFile: File?) {
        execute {
            if (parentFile == rootDoc) {
                parentFile?.listFiles()?.toList()
            } else {
                val list = arrayListOf(parentFile)
                parentFile?.listFiles()?.let {
                    list.addAll(it)
                }
                list
            }
        }.onStart {
            filesLiveData.postValue(emptyList())
        }.onSuccess {
            filesLiveData.postValue(it ?: emptyList())
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }


}