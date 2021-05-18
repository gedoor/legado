package io.legado.app.ui.association

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.help.IntentDataHelp
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.readText
import java.io.File

class FileAssociationViewModel(application: Application) : BaseViewModel(application) {

    val successLiveData = MutableLiveData<Intent>()
    val errorLiveData = MutableLiveData<String>()

    @Suppress("BlockingMethodInNonBlockingContext")
    fun dispatchIndent(uri: Uri) {
        execute {
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                val content = if (uri.scheme == "file") {
                    File(uri.path.toString()).readText()
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.readText(context)
                }
                if (content != null) {
                    if (content.isJsonObject() || content.isJsonArray()) {
                        //暂时根据文件内容判断属于什么
                        when {
                            content.contains("bookSourceUrl") -> {
                                successLiveData.postValue(
                                    Intent(
                                        context,
                                        ImportBookSourceActivity::class.java
                                    ).apply {
                                        val dataKey = IntentDataHelp.putData(content)
                                        putExtra("dataKey", dataKey)
                                    }
                                )
                                return@execute
                            }
                            content.contains("sourceUrl") -> {
                                successLiveData.postValue(
                                    Intent(
                                        context,
                                        ImportRssSourceActivity::class.java
                                    ).apply {
                                        val dataKey = IntentDataHelp.putData(content)
                                        putExtra("dataKey", dataKey)
                                    }
                                )
                                return@execute
                            }
                            content.contains("pattern") -> {
                                successLiveData.postValue(
                                    Intent(
                                        context,
                                        ImportReplaceRuleActivity::class.java
                                    ).apply {
                                        val dataKey = IntentDataHelp.putData(content)
                                        putExtra("dataKey", dataKey)
                                    }
                                )
                                return@execute
                            }
                        }
                    }
                    val book = if (uri.scheme == "content") {
                        LocalBook.importFile(uri)
                    } else {
                        LocalBook.importFile(uri)
                    }
                    val intent = Intent(context, ReadBookActivity::class.java)
                    intent.putExtra("bookUrl", book.bookUrl)
                    successLiveData.postValue(intent)
                } else {
                    throw Exception("文件不存在")
                }
            }
        }.onError {
            it.printStackTrace()
            errorLiveData.postValue(it.localizedMessage)
        }
    }
}