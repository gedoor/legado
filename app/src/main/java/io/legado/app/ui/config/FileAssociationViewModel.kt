package io.legado.app.ui.config

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import io.legado.app.base.BaseViewModel
import io.legado.app.model.localBook.LocalBook
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.isJsonObject
import org.jetbrains.anko.toast
import java.io.File

class FileAssociationViewModel(application: Application) : BaseViewModel(application) {
    fun dispatchIndent(uri:Uri):Intent?{
            val url:String
            //如果是普通的url，需要根据返回的内容判断是什么
            if(uri.scheme == "file" || uri.scheme == "content"){
                val file = File(uri.path.toString())
                var scheme = ""
                if (file.exists()) {
                    val content = file.readText()
                    if (content.isJsonObject() || content.isJsonArray()){
                        //暂时根据文件内容判断属于什么
                        if (content.contains("bookSourceUrl")){
                            scheme = "booksource"
                        }else if (content.contains("sourceUrl")){
                            scheme = "rsssource"
                        }else if (content.contains("pattern")){
                            scheme = "replace"
                        }
                    }
                    if (TextUtils.isEmpty(scheme)){
                        execute{
                            LocalBook.importFile(uri.path.toString())
                            toast("添加本地文件成功${uri.path}")
                        }

                        return null
                    }
                }
                else{
                    toast("文件不存在")
                    return null
                }

                url = "yuedu://${scheme}/importonline?src=${uri.path}"
            }
            else if (uri.scheme == "yuedu"){
                url = uri.toString()
            }
            else{
                url = "yuedu://booksource/importonline?src=${uri.path}"
            }
            val data = Uri.parse(url)
            val newIndent = Intent(Intent.ACTION_VIEW)
            newIndent.data = data;
            return  newIndent
    }
}