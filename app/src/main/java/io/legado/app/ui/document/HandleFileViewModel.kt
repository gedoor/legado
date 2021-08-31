package io.legado.app.ui.document

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.DirectLinkUpload
import io.legado.app.utils.FileUtils
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.writeBytes
import java.io.File

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

    fun saveToLocal(uri: Uri, fileName: String, data: ByteArray, success: (uri: Uri) -> Unit) {
        execute {
            return@execute if (uri.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(context, uri)!!
                doc.findFile(fileName)?.delete()
                val newDoc = doc.createFile("", fileName)
                newDoc!!.writeBytes(context, data)
                newDoc.uri
            } else {
                val file = File(uri.path!!)
                val newFile = FileUtils.createFileIfNotExist(file, fileName)
                newFile.writeBytes(data)
                Uri.fromFile(newFile)
            }
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(it.localizedMessage)
        }.onSuccess {
            success.invoke(it)
        }
    }

}