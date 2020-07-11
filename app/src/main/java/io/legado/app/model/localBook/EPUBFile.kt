package io.legado.app.model.localBook

import android.net.Uri
import android.text.TextUtils
import io.legado.app.App
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.htmlFormat
import io.legado.app.utils.isContentPath
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

class EPUBFile(val book: io.legado.app.data.entities.Book) {

    companion object {
        private var eFile: EPUBFile? = null

        @Synchronized
        private fun getEFile(book: io.legado.app.data.entities.Book): EPUBFile {
            if (eFile == null || eFile?.book?.bookUrl != book.bookUrl) {
                eFile = EPUBFile(book)
                return eFile!!
            }
            return eFile!!
        }

        @Synchronized
        fun getChapterList(book: io.legado.app.data.entities.Book): ArrayList<BookChapter> {
            return getEFile(book).getChapterList()
        }

        @Synchronized
        fun getContent(book: io.legado.app.data.entities.Book, chapter: BookChapter): String? {
            return getEFile(book).getContent(chapter)
        }
    }

    private var epubBook: Book? = null
    private var mCharset: Charset = Charset.defaultCharset()

    init {
        try {
            val epubReader = EpubReader()
            val inputStream = if (book.bookUrl.isContentPath()) {
                val uri = Uri.parse(book.bookUrl)
                App.INSTANCE.contentResolver.openInputStream(uri)
            } else {
                File(book.bookUrl).inputStream()
            }
            epubBook = epubReader.readEpub(inputStream)
        } catch (e: Exception) {
        }
    }

    private fun getContent(chapter: BookChapter): String? {
        epubBook?.let { eBook ->
            val resource = eBook.resources.getByHref(chapter.url)
            val content = StringBuilder()
            val doc = Jsoup.parse(String(resource.data, mCharset))
            val elements = doc.allElements
            for (element in elements) {
                val contentEs = element.textNodes()
                for (i in contentEs.indices) {
                    val text = contentEs[i].text().trim { it <= ' ' }.htmlFormat()
                    if (elements.size > 1) {
                        if (text.isNotEmpty()) {
                            if (content.isNotEmpty()) {
                                content.append("\r\n")
                            }
                            content.append("\u3000\u3000").append(text)
                        }
                    } else {
                        content.append(text)
                    }
                }
            }
            return content.toString()
        }
        return null
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        epubBook?.let { eBook ->
            val metadata = eBook.metadata
            book.name = metadata.firstTitle
            if (metadata.authors.size > 0) {
                val author =
                    metadata.authors[0].toString().replace("^, |, $".toRegex(), "")
                book.author = author
            }
            if (metadata.descriptions.size > 0) {
                book.intro = Jsoup.parse(metadata.descriptions[0]).text()
            }

            val refs = eBook.tableOfContents.tocReferences
            if (refs == null || refs.isEmpty()) {
                val spineReferences = eBook.spine.spineReferences
                var i = 0
                val size = spineReferences.size
                while (i < size) {
                    val resource =
                        spineReferences[i].resource
                    var title = resource.title
                    if (TextUtils.isEmpty(title)) {
                        try {
                            val doc =
                                Jsoup.parse(String(resource.data, mCharset))
                            val elements = doc.getElementsByTag("title")
                            if (elements.size > 0) {
                                title = elements[0].text()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    val chapter = BookChapter()
                    chapter.index = i
                    chapter.bookUrl = book.bookUrl
                    chapter.url = resource.href
                    if (i == 0 && title.isEmpty()) {
                        chapter.title = "封面"
                    } else {
                        chapter.title = title
                    }
                    chapterList.add(chapter)
                    i++
                }
            } else {
                parseMenu(chapterList, refs, 0)
                for (i in chapterList.indices) {
                    chapterList[i].index = i
                }
            }
        }
        return chapterList
    }

    private fun parseMenu(
        chapterList: ArrayList<BookChapter>,
        refs: List<TOCReference>?,
        level: Int
    ) {
        if (refs == null) return
        for (ref in refs) {
            if (ref.resource != null) {
                val chapter = BookChapter()
                chapter.bookUrl = book.bookUrl
                chapter.title = ref.title
                chapter.url = ref.completeHref
                chapterList.add(chapter)
            }
            if (ref.children != null && ref.children.isNotEmpty()) {
                parseMenu(chapterList, ref.children, level + 1)
            }
        }
    }


}