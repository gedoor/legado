package io.legado.app.ui.association

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.help.storage.OldReplace
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.readText
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toText
import java.io.File

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<ArrayList<ReplaceRule>>()

    private val allRules = arrayListOf<ReplaceRule>()

    fun importFromFilePath(path: String) {
        execute {
            val content = if (path.isContentScheme()) {
                //在前面被解码了，如果不进行编码，中文会无法识别
                val newPath = Uri.encode(path, ":/.")
                DocumentFile.fromSingleUri(context, Uri.parse(newPath))?.readText(context)
            } else {
                val file = File(path)
                if (file.exists()) {
                    file.readText()
                } else {
                    null
                }
            }
            if (content != null) {
                import(content)
            } else {
                errorLiveData.postValue(context.getString(R.string.error_read_file))
            }
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(context.getString(R.string.error_read_file))
        }
    }

    fun import(text: String) {
        execute {
            if (text.isAbsUrl()) {
                RxHttp.get(text).toText("utf-8").await().let {
                    val rules = OldReplace.jsonToReplaceRules(it)
                    allRules.addAll(rules)
                }
            } else {
                val rules = OldReplace.jsonToReplaceRules(text)
                allRules.addAll(rules)
            }
        }.onError {
            errorLiveData.postValue(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            successLiveData.postValue(allRules)
        }
    }
}