package io.legado.app.ui.book.bookmark

import android.app.Application
import android.net.Uri
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.utils.FileDoc
import io.legado.app.utils.GSON
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.openOutputStream
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeToOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AllBookmarkViewModel(application: Application) : BaseViewModel(application) {


    /**
     * 导出书签
     */
    fun exportBookmark(treeUri: Uri) {
        execute {
            val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
            val fileName = "bookmark-${dateFormat.format(Date())}.json"
            val dirDoc = FileDoc.fromUri(treeUri, true)
            dirDoc.createFileIfNotExist(fileName).openOutputStream().getOrThrow().use {
                GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
            }
        }.onError {
            AppLog.put("导出失败\n${it.localizedMessage}", it, true)
        }.onSuccess {
            context.toastOnUi("导出成功")
        }
    }


    fun exportBookmarkMd(treeUri: Uri) {
        execute {
            val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
            val fileName = "bookmark-${dateFormat.format(Date())}.md"
            val dirDoc = FileDoc.fromUri(treeUri, true)
            val fileDoc = dirDoc.createFileIfNotExist(fileName).openOutputStream().getOrThrow()
            fileDoc.use { outputStream ->
                var name = ""
                var author = ""
                appDb.bookmarkDao.all.forEach {
                    if (it.bookName != name && it.bookAuthor != author) {
                        name = it.bookName
                        author = it.bookAuthor
                        outputStream.write("## ${it.bookName} ${it.bookAuthor}\n\n".toByteArray())
                    }
                    outputStream.write("#### ${it.chapterName}\n\n".toByteArray())
                    outputStream.write("###### 原文\n ${it.bookText}\n\n".toByteArray())
                    outputStream.write("###### 摘要\n ${it.content}\n\n".toByteArray())
                }
            }
        }.onError {
            AppLog.put("导出失败\n${it.localizedMessage}", it, true)
        }.onSuccess {
            context.toastOnUi("导出成功")
        }
    }

}