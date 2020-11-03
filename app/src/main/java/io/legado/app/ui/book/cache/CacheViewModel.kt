package io.legado.app.ui.book.cache

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.constant.PreferKey
import io.legado.app.data.entities.Book
import io.legado.app.help.BookHelp
import io.legado.app.help.storage.WebDavHelp
import io.legado.app.utils.*
import java.io.File


class CacheViewModel(application: Application) : BaseViewModel(application) {


    fun export(path: String, book: Book, finally: (msg: String) -> Unit) {
        execute {
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)?.let {
                    export(it, book)
                }
            } else {
                export(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            finally(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            finally(context.getString(R.string.success))
        }
    }

    private fun export(doc: DocumentFile, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        val content = getAllContents(book)
        DocumentUtils.createFileIfNotExist(doc, filename)
            ?.writeText(context, content)
        if(App.INSTANCE.getPrefBoolean(PreferKey.webDavCacheBackup,false)) {
            FileUtils.createFileIfNotExist(
                File(FileUtils.getCachePath()),
                filename
            ).writeText(content) // 写出文件到cache目录
            // 导出到webdav
            WebDavHelp.exportWebDav(FileUtils.getCachePath(), filename)
            // 上传完删除cache文件
            FileUtils.deleteFile("${FileUtils.getCachePath()}${File.separator}${filename}")
        }
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    if (matcher.find()) {
                        var src = matcher.group(1)
                        src = NetworkUtils.getAbsoluteURL(chapter.url, src)
                        src?.let {
                            val vFile = BookHelp.getImage(book, src)
                            if (vFile.exists()) {
                                DocumentUtils.createFileIfNotExist(doc,
                                    "${index}-${MD5Utils.md5Encode16(src)}.jpg",
                                    subDirs = arrayOf("${book.name}_${book.author}",
                                        "images",
                                        chapter.title))
                                    ?.writeBytes(context, vFile.readBytes())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun export(file: File, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        FileUtils.createFileIfNotExist(file, filename)
            .writeText(getAllContents(book))
        if(App.INSTANCE.getPrefBoolean(PreferKey.webDavCacheBackup,false)) {
            WebDavHelp.exportWebDav(file.absolutePath, filename) // 导出到webdav
        }
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                content?.split("\n")?.forEachIndexed { index, text ->
                    val matcher = AppPattern.imgPattern.matcher(text)
                    if (matcher.find()) {
                        var src = matcher.group(1)
                        src = NetworkUtils.getAbsoluteURL(chapter.url, src)
                        src?.let {
                            val vFile = BookHelp.getImage(book, src)
                            if (vFile.exists()) {
                                FileUtils.createFileIfNotExist(file,
                                    "${book.name}_${book.author}",
                                    "images",
                                    chapter.title,
                                    "${index}-${MD5Utils.md5Encode16(src)}.jpg")
                                    .writeBytes(vFile.readBytes())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getAllContents(book: Book): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(book.name)
            .append("\n")
            .append(context.getString(R.string.author_show, book.author))
        App.db.bookChapterDao().getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let {
                stringBuilder.append("\n\n")
                    .append(chapter.title)
                    .append("\n")
                    .append(it)
            }
        }
        return stringBuilder.toString()
    }
}