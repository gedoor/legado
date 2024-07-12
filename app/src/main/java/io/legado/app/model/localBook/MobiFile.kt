package io.legado.app.model.localBook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.BookHelp
import io.legado.app.lib.mobi.KF6Book
import io.legado.app.lib.mobi.KF8Book
import io.legado.app.lib.mobi.MobiBook
import io.legado.app.lib.mobi.MobiReader
import io.legado.app.lib.mobi.entities.TOC
import io.legado.app.utils.FileUtils
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.printOnDebug
import org.jsoup.Jsoup
import java.io.FileOutputStream
import java.io.InputStream

class MobiFile(var book: Book) {

    companion object : BaseLocalBookParse {
        private var mFile: MobiFile? = null
        private val xmlDeclarationRegex = "<\\?xml[^>]*>".toRegex()
        private val doctypeDeclarationRegex = "<!DOCTYPE[^>]*>".toRegex()

        @Synchronized
        private fun getMFile(book: Book): MobiFile {
            if (mFile == null || mFile?.book?.bookUrl != book.bookUrl) {
                mFile = MobiFile(book)
                return mFile!!
            }
            mFile?.book = book
            return mFile!!
        }

        @Synchronized
        override fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getMFile(book).getChapterList()
        }

        @Synchronized
        override fun getContent(book: Book, chapter: BookChapter): String? {
            return getMFile(book).getContent(chapter)
        }

        @Synchronized
        override fun getImage(book: Book, href: String): InputStream? {
            return getMFile(book).getImage(href)
        }

        @Synchronized
        override fun upBookInfo(book: Book) {
            return getMFile(book).upBookInfo()
        }

        fun clear() {
            mFile = null
        }
    }

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var mobiBook: MobiBook? = null
        get() {
            if (field == null || fileDescriptor == null) {
                field = readMobi()
            }
            return field
        }

    private fun readMobi(): MobiBook? {
        return kotlin.runCatching {
            BookHelp.getBookPFD(book)?.let {
                fileDescriptor = it
                MobiReader().readMobi(it)
            }
        }.onFailure {
            AppLog.put("读取Mobi文件失败\n${it.localizedMessage}", it)
            it.printOnDebug()
        }.getOrThrow()
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        return when (val book = mobiBook) {
            is KF8Book -> getChapterListKF8(book)
            is KF6Book -> getChapterListKF6(book)
            else -> error("impossible condition")
        }
    }

    private fun getChapterListKF6(kF6Book: KF6Book): ArrayList<BookChapter> {
        val chapterList = arrayListOf<BookChapter>()
        val toc = kF6Book.toc

        if (kF6Book.sectionIdMap[0] == null) {
            val section = kF6Book.sections.firstOrNull()
            if (section != null) {
                val chapter = BookChapter()
                val content = kF6Book.getSectionText(section)
                val soup = Jsoup.parse(content)
                val title = soup.getElementsByTag("title").first()?.text() ?: "卷首"
                chapter.bookUrl = book.bookUrl
                chapter.title = title
                chapter.url = "0:" + section.href
                chapterList.add(chapter)
            }
        }

        fun append(ref: TOC) {
            val chapter = BookChapter()
            chapter.bookUrl = book.bookUrl
            chapter.title = ref.label
            chapter.url = "${chapterList.size}:${ref.href}"
            chapter.isVolume = ref.subitems != null
            val lastChapter = chapterList.lastOrNull()
            if (lastChapter != null &&
                lastChapter.isVolume &&
                lastChapter.url.substringAfter(":") == chapter.url.substringAfter(":")
            ) {
                lastChapter.url = "skip:" + lastChapter.url
            }
            lastChapter?.putVariable("nextUrl", chapter.url)
            chapterList.add(chapter)
            ref.subitems?.forEach(::append)
        }

        toc?.forEach(::append)

        return chapterList
    }

    private fun getChapterListKF8(kf8Book: KF8Book): ArrayList<BookChapter> {
        val chapterList = arrayListOf<BookChapter>()
        val toc = kf8Book.toc

        if (kf8Book.sectionIdMap[0] == null) {
            val section = kf8Book.sections.firstOrNull { it.href.isNotEmpty() }
            if (section != null) {
                val chapter = BookChapter()
                val content = kf8Book.getSectionText(section)
                val soup = Jsoup.parse(content)
                val title = soup.getElementsByTag("title").first()?.text() ?: "卷首"
                chapter.bookUrl = book.bookUrl
                chapter.title = title
                chapter.url = "0:" + section.href
                chapterList.add(chapter)
            }
        }

        fun append(ref: TOC) {
            val chapter = BookChapter()
            chapter.bookUrl = book.bookUrl
            chapter.title = ref.label
            chapter.url = "${chapterList.size}:${ref.href}"
            chapter.isVolume = ref.subitems != null
            val lastChapter = chapterList.lastOrNull()
            if (lastChapter != null &&
                lastChapter.isVolume &&
                lastChapter.url.substringAfter(":") == chapter.url.substringAfter(":")
            ) {
                lastChapter.url = "skip:" + lastChapter.url
            }
            lastChapter?.putVariable("nextUrl", chapter.url)
            chapterList.add(chapter)
            ref.subitems?.forEach(::append)
        }

        toc?.forEach(::append)

        return chapterList
    }

    private fun getContent(chapter: BookChapter): String? {
        return when (val book = mobiBook) {
            is KF8Book -> getContentKF8(book, chapter)
            is KF6Book -> getContentKF6(book, chapter)
            else -> error("impossible condition")
        }
    }

    private fun getContentKF6(kf6Book: KF6Book, chapter: BookChapter): String? {
        if (chapter.isVolume && chapter.url.startsWith("skip:")) return ""
        var section = kf6Book.getSectionByHref(chapter.url) ?: return null
        val nextSectionHref = chapter.getVariable("nextUrl")

        val sb = StringBuilder()
        sb.append(kf6Book.getSectionText(section))
        while (true) {
            section = section.next ?: break
            if (section.href == nextSectionHref) {
                break
            }
            if (kf6Book.sectionIdMap[section.index] != null) {
                break
            }
            sb.append(kf6Book.getSectionText(section))
        }

        val soup = Jsoup.parse(sb.toString())

        soup.select("title").remove()
        soup.select("[style*=display:none]").remove()
        soup.select("img[recindex]").forEach {
            val recindex = it.attr("recindex")
            it.clearAttributes()
            it.attr("src", "recindex:$recindex")
        }

        return format(soup.outerHtml())
    }

    private fun getContentKF8(kf8Book: KF8Book, chapter: BookChapter): String? {
        if (chapter.isVolume && chapter.url.startsWith("skip:")) return ""
        var section = kf8Book.getSectionByHref(chapter.url) ?: return null
        val nextSectionHref = chapter.getVariable("nextUrl")
        val nextPos = kf8Book.parsePosURI(nextSectionHref)

        val sb = StringBuilder()
        sb.append(kf8Book.getTextByHref(chapter.url, nextSectionHref))
        while (true) {
            if (nextPos != null && section.frags.any { it.index == nextPos.fid }) {
                break
            }
            section = section.next ?: break
            if (section.linear) {
                continue
            }
            if (section.href == nextSectionHref) {
                break
            }
            if (kf8Book.sectionIdMap[section.index] != null) {
                break
            }
            sb.append(kf8Book.getSectionText(section))
        }

        val soup = Jsoup.parse(sb.toString())

        soup.select("title").remove()
        soup.select("[style*=display:none]").remove()

        return format(soup.outerHtml())
    }

    private fun format(html: String): String {
        return HtmlFormatter.formatKeepImg(html)
            .replace(xmlDeclarationRegex, "")
            .replace(doctypeDeclarationRegex, "")
    }

    private fun getImage(href: String): InputStream? {
        return when (val book = mobiBook) {
            is KF8Book -> getImageKF8(book, href)
            is KF6Book -> getImageKF6(book, href)
            else -> error("impossible condition")
        }
    }

    private fun getImageKF6(kf6Book: KF6Book, href: String): InputStream? {
        return kf6Book.getResourceByHref(href)?.inputStream()
    }

    private fun getImageKF8(kf8Book: KF8Book, href: String): InputStream? {
        return kf8Book.getResourceByHref(href)?.inputStream()
    }

    private fun upBookCover() {
        try {
            mobiBook?.let {
                if (book.coverUrl.isNullOrEmpty()) {
                    book.coverUrl = LocalBook.getCoverPath(book)
                }
                it.getCover()?.let { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val file = FileUtils.createFileIfNotExist(book.coverUrl!!)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.put("加载书籍封面失败\n${e.localizedMessage}", e)
            e.printOnDebug()
        }
    }

    private fun upBookInfo() {
        if (mobiBook == null) {
            mFile = null
            book.intro = "书籍导入异常"
        } else {
            upBookCover()
            val metadata = mobiBook!!.metadata
            book.name = metadata.title
            if (book.name.isEmpty()) {
                book.name = book.originName.replace("(?i)\\.(mobi|azw3)$".toRegex(), "")
            }
            if (metadata.author.isNotEmpty()) {
                book.author = metadata.author.first()
            }
            if (metadata.description.isNotBlank()) {
                book.intro = HtmlFormatter.format(metadata.description)
            }
        }
    }

}
