package io.legado.app.model.localBook

import android.net.Uri
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter

import io.legado.app.utils.FileUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFilesDir
import io.legado.app.utils.isContentScheme
import me.ag2s.umdlib.domain.UmdBook
import me.ag2s.umdlib.umd.UmdReader
import splitties.init.appCtx
import java.io.File
import java.io.InputStream
import java.util.ArrayList

class UmdFile(var book: Book) {
    companion object {
        private var eFile: UmdFile? = null

        @Synchronized
        private fun getEFile(book: Book): UmdFile {
            //BookHelp.getEpubFile(book)

            if (eFile == null || eFile?.book?.bookUrl != book.bookUrl) {
                eFile = UmdFile(book)
                //对于Epub文件默认不启用替换
                //book.setUseReplaceRule(false)
                return eFile!!
            }
            eFile?.book = book
            return eFile!!
        }

        @Synchronized
        fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getEFile(book).getChapterList()
        }

        @Synchronized
        fun getContent(book: Book, chapter: BookChapter): String? {
            return getEFile(book).getContent(chapter)
        }

        @Synchronized
        fun getImage(
            book: Book,
            href: String
        ): InputStream? {
            return getEFile(book).getImage(href)
        }


        @Synchronized
        fun upBookInfo(book: Book) {
            return getEFile(book).upBookInfo()
        }
    }



    private var umdBook: UmdBook? = null
        get() {
            if (field != null) {
                return field
            }
            field = readUmd()
            return field
        }



    init {
        try {
            umdBook?.let {
                if (book.coverUrl.isNullOrEmpty()) {
                    book.coverUrl = FileUtils.getPath(
                        appCtx.externalFilesDir,
                        "covers",
                        "${MD5Utils.md5Encode16(book.bookUrl)}.jpg"
                    )
                }
                if (!File(book.coverUrl!!).exists()) {
                    FileUtils.writeBytes(book.coverUrl!!,it.cover.coverData)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun readUmd(): UmdBook? {
        val input= if (book.bookUrl.isContentScheme()) {
            val uri = Uri.parse(book.bookUrl)
            appCtx.contentResolver.openInputStream(uri)
        } else {
            File(book.bookUrl).inputStream()
        }
        return UmdReader().read(input)
    }

    private fun upBookInfo() {
        if(umdBook==null){
            eFile = null
            book.intro = "书籍导入异常"
        }else{
            val hd= umdBook!!.header
            book.name=hd.title;
            book.author=hd.author;
            book.kind=hd.bookType;
        }
    }
    private fun getContent(chapter: BookChapter): String? {
        return umdBook?.chapters?.getContentString(chapter.index)
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        umdBook?.chapters?.titles?.forEachIndexed { index, bytes ->
            val title = umdBook!!.chapters.getTitle(index)
            val chapter = BookChapter()
            chapter.title=title;
            chapter.index = index
            chapter.bookUrl = book.bookUrl
            chapter.url = index.toString();
            chapterList.add(chapter)
        }
        return chapterList
    }

    private fun getImage(href: String): InputStream? {
        TODO("Not yet implemented")
    }


}