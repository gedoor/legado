package io.legado.app.ui.book.cache

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.AppConfig
import io.legado.app.help.BookHelp
import io.legado.app.help.ContentProcessor
import io.legado.app.help.storage.BookWebDav
import io.legado.app.utils.*
import me.ag2s.epublib.domain.*
import me.ag2s.epublib.epub.EpubWriter
import me.ag2s.epublib.util.ResourceUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset


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

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun export(doc: DocumentFile, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        DocumentUtils.delete(doc, filename)
        DocumentUtils.createFileIfNotExist(doc, filename)?.let { bookDoc ->
            val stringBuilder = StringBuilder()
            context.contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
                getAllContents(book) {
                    bookOs.write(it.toByteArray(Charset.forName(AppConfig.exportCharset)))
                    stringBuilder.append(it)
                }
            }
            if (AppConfig.exportToWebDav) {
                // 导出到webdav
                val byteArray =
                    stringBuilder.toString().toByteArray(Charset.forName(AppConfig.exportCharset))
                BookWebDav.exportWebDav(byteArray, filename)
            }
        }
        getSrcList(book).forEach {
            val vFile = BookHelp.getImage(book, it.third)
            if (vFile.exists()) {
                DocumentUtils.createFileIfNotExist(
                    doc,
                    "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg",
                    subDirs = arrayOf("${book.name}_${book.author}", "images", it.first)
                )?.writeBytes(context, vFile.readBytes())
            }
        }
    }

    private suspend fun export(file: File, book: Book) {
        val filename = "${book.name} by ${book.author}.txt"
        val bookPath = FileUtils.getPath(file, filename)
        val bookFile = FileUtils.createFileWithReplace(bookPath)
        val stringBuilder = StringBuilder()
        getAllContents(book) {
            bookFile.appendText(it, Charset.forName(AppConfig.exportCharset))
            stringBuilder.append(it)
        }
        if (AppConfig.exportToWebDav) {
            val byteArray =
                stringBuilder.toString().toByteArray(Charset.forName(AppConfig.exportCharset))
            BookWebDav.exportWebDav(byteArray, filename) // 导出到webdav
        }
        getSrcList(book).forEach {
            val vFile = BookHelp.getImage(book, it.third)
            if (vFile.exists()) {
                FileUtils.createFileIfNotExist(
                    file,
                    "${book.name}_${book.author}",
                    "images",
                    it.first,
                    "${it.second}-${MD5Utils.md5Encode16(it.third)}.jpg"
                ).writeBytes(vFile.readBytes())
            }
        }
    }

    private fun getAllContents(book: Book, append: (text: String) -> Unit) {
        val useReplace = AppConfig.exportUseReplace
        val contentProcessor = ContentProcessor(book.name, book.origin)
        append("${book.name}\n${context.getString(R.string.author_show, book.author)}")
        appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                val content1 = contentProcessor
                    .getContent(book, chapter.title, content ?: "null", false, useReplace)
                    .joinToString("\n")
                append.invoke("\n\n$content1")
            }
        }
    }

    private fun getSrcList(book: Book): ArrayList<Triple<String, Int, String>> {
        val srcList = arrayListOf<Triple<String, Int, String>>()
        appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter)?.let { content ->
                content.split("\n").forEachIndexed { index, text ->
                    val matches = AppPattern.imgPattern.toRegex().findAll(input = text)
                    matches.forEach { matchResult ->
                        matchResult.groupValues[1].let {
                            val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                            srcList.add(Triple(chapter.title, index, src))
                        }
                    }
                }
            }
        }
        return srcList
    }
    //////////////////Start EPUB
    /**
     * 导出Epub
     */
    fun exportEPUB(path: String, book: Book, finally: (msg: String) -> Unit) {
        execute {
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)?.let {
                    exportEpub(it, book)
                }
            } else {
                exportEpub(FileUtils.createFolderIfNotExist(path), book)
            }
        }.onError {
            finally(it.localizedMessage ?: "ERROR")
        }.onSuccess {
            finally(context.getString(R.string.success))
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun exportEpub(doc: DocumentFile, book: Book) {
        val filename = "${book.name} by ${book.author}.epub"
        DocumentUtils.delete(doc, filename)
        val epubBook = EpubBook()
        epubBook.version = "2.0"
        //set metadata
        setEpubMetadata(book, epubBook)
        //set cover
        setCover(book, epubBook)

        //set css
        setCSS(epubBook)
        //设置正文
        setEpubContent(book, epubBook)

        DocumentUtils.createFileIfNotExist(doc, filename)?.let { bookDoc ->
            context.contentResolver.openOutputStream(bookDoc.uri, "wa")?.use { bookOs ->
                EpubWriter().write(epubBook, bookOs)
            }

        }
    }

    private fun exportEpub(file: File, book: Book) {
        val filename = "${book.name} by ${book.author}.epub"
        val epubBook = EpubBook()
        epubBook.version = "2.0"
        //set metadata
        setEpubMetadata(book, epubBook)
        //set cover
        setCover(book, epubBook)
        //set css
        setCSS(epubBook)


        val bookPath = FileUtils.getPath(file, filename)
        val bookFile = FileUtils.createFileWithReplace(bookPath)
        //设置正文
        setEpubContent(book, epubBook)
        EpubWriter().write(epubBook, FileOutputStream(bookFile))
    }

    private fun setCSS(epubBook: EpubBook) {
        //set css
        epubBook.resources.add(
            Resource(
                "body,div{background:white;outline:none;width:100%;}h2{color:#005a9c;text-align:left;}p{text-indent:2em;text-align:justify;}img{display:inline-block;width:100%;height:auto;max-width: 100%;max-height:100%;}".encodeToByteArray(),
                "css/style.css"
            )
        )
    }

    private fun setCover(book: Book, epubBook: EpubBook) {

        Glide.with(context)
            .asBitmap()
            .load(book.getDisplayCover())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val stream = ByteArrayOutputStream()
                    resource.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val byteArray: ByteArray = stream.toByteArray()
                    resource.recycle()
                    stream.close()
                    epubBook.coverImage = Resource(byteArray, "cover.jpg")
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
    }


    private fun setEpubContent(book: Book, epubBook: EpubBook) {
        val useReplace = AppConfig.exportUseReplace
        val contentProcessor = ContentProcessor(book.name, book.origin)
        appDb.bookChapterDao.getChapterList(book.bookUrl).forEach { chapter ->
            BookHelp.getContent(book, chapter).let { content ->
                var content1 = fixPic(epubBook, book, content ?: "null", chapter)
                content1 = contentProcessor
                    .getContent(book, "", content1, false, useReplace)
                    .joinToString("\n")
                epubBook.addSection(
                    chapter.title,
                    ResourceUtil.createHTMLResource(chapter.title, content1)
                )
            }
        }
    }

    private fun setPic(src: String, book: Book, epubBook: EpubBook) {
        val vFile = BookHelp.getImage(book, src)
        if (vFile.exists()) {
            val img = Resource(FileInputStream(vFile), MD5Utils.md5Encode16(src) + ".jpg")
            epubBook.resources.add(img)
        }
    }

    private fun fixPic(
        epubBook: EpubBook,
        book: Book,
        content: String,
        chapter: BookChapter
    ): String {
        val data = StringBuilder("")
        content.split("\n").forEach { text ->
            var text1 = text
            val matches = AppPattern.imgPattern.toRegex().findAll(input = text)
            matches.forEach { matchResult ->
                matchResult.groupValues[1].let {
                    val src = NetworkUtils.getAbsoluteURL(chapter.url, it)
                    setPic(src, book, epubBook)
                    text1 = text1.replace(src, MD5Utils.md5Encode16(src) + ".jpg")

                }
            }

            data.append(text1).append("\n")
        }
        return data.toString()
    }

    private fun setEpubMetadata(book: Book, epubBook: EpubBook) {
        val metadata = Metadata()
        metadata.titles.add(book.name)//书籍的名称
        metadata.authors.add(Author(book.getRealAuthor()))//书籍的作者
        metadata.language = "zh"//数据的语言
        metadata.dates.add(Date())//数据的创建日期
        metadata.publishers.add("Legado APP")//数据的创建者
        metadata.descriptions.add(book.getDisplayIntro())//书籍的简介
        //metadata.subjects.add("")//书籍的主题，在静读天下里面有使用这个分类书籍
        epubBook.metadata = metadata
    }

    //////end of EPUB


}