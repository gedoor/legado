package io.legado.app.ui.document

import android.app.Application
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.DirectLinkUpload

class HandleFileViewModel(application: Application) : BaseViewModel(application) {

    val errorLiveData = MutableLiveData<String>()

    fun upload(fileName: String, byteArray: ByteArray, success: (url: String) -> Unit) {
        execute {
            DirectLinkUpload.upLoad(fileName, byteArray)
        }.onSuccess {
            success.invoke(it)
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(it.localizedMessage)
        }
    }

}