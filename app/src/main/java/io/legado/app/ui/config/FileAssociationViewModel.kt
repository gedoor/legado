package io.legado.app.ui.config

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.documentfile.provider.DocumentFile
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.readText
import java.io.File

class FileAssociationViewModel(application: Application) : BaseViewModel(application) {

    fun dispatchIndent(uri: Uri): Intent? {
        try {
            val url: String
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.scheme == "file" || uri.scheme == "content") {
                val content = if (uri.scheme == "file") {
                    val file = File(uri.path.toString())
                    if (file.exists()) {
                        file.readText()
                    } else {
                        null
                    }
                } else {
                    DocumentFile.fromSingleUri(context, uri)?.readText(context)
                }
                var scheme = ""
                if (content != null) {
                    if (content.isJsonObject() || content.isJsonArray()) {
                        //暂时根据文件内容判断属于什么
                        when {
                            content.contains("bookSourceUrl") -> {
                                scheme = "booksource"
                            }
                            content.contains("sourceUrl") -> {
                                scheme = "rsssource"
                            }
                            content.contains("pattern") -> {
                                scheme = "replace"
                            }
                        }
                    }
                    if (TextUtils.isEmpty(scheme)) {
                        execute {
                            if (uri.scheme == "content") {
                                LocalBook.importFile(uri.toString())
                            } else {
                                LocalBook.importFile(uri.path.toString())
                            }
                            toast("添加本地文件成功${uri.path}")
                        }
                        return null
                    }
                } else {
                    toast("文件不存在")
                    return null
                }
                // content模式下，需要传递完整的路径，方便后续解析
                url = if (uri.scheme == "content") {
                    "legado://${scheme}/importonline?src=$uri"
                } else {
                    "legado://${scheme}/importonline?src=${uri.path}"
                }

            } else if (uri.scheme == "yuedu") {
                url = uri.toString()
            } else {
                url = "legado://booksource/importonline?src=${uri.path}"
            }
            val data = Uri.parse(url)
            val newIndent = Intent(Intent.ACTION_VIEW)
            newIndent.data = data
            return newIndent
        } catch (e: Exception) {
            e.printStackTrace()
            toast(e.localizedMessage)
            return null
        }
    }
}