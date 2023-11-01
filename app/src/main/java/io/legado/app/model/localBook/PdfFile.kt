package io.legado.app.model.localBook

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.getLocalUri
import io.legado.app.utils.*
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.ceil


class PdfFile(var book: Book) {
    companion object : BaseLocalBookParse {
        private var pFile: PdfFile? = null

        /**
         * pdf分页尺寸
         */
        const val PAGE_SIZE = 10

        @Synchronized
        private fun getPFile(book: Book): PdfFile {
            if (pFile == null || pFile?.book?.bookUrl != book.bookUrl) {
                pFile = PdfFile(book)
                return pFile!!
            }
            pFile?.book = book
            return pFile!!
        }

        @Synchronized
        override fun upBookInfo(book: Book) {
            getPFile(book).upBookInfo()
        }

        @Synchronized
        override fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getPFile(book).getChapterList()
        }

        @Synchronized
        override fun getContent(book: Book, chapter: BookChapter): String? {
            return getPFile(book).getContent(chapter)
        }

        @Synchronized
        override fun getImage(book: Book, href: String): InputStream? {
            return getPFile(book).getImage(href)
        }

    }

    /**
     *持有引用，避免被回收
     */
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
        get() {
            if (field != null && fileDescriptor != null) {
                return field
            }
            field = readPdf()
            return field
        }


    init {
        try {
            pdfRenderer?.let { renderer ->
                if (book.coverUrl.isNullOrEmpty()) {
                    book.coverUrl = LocalBook.getCoverPath(book)
                }
                if (!File(book.coverUrl!!).exists()) {

                    FileOutputStream(FileUtils.createFileIfNotExist(book.coverUrl!!)).use { out ->
                        openPdfPage(renderer, 0)?.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        out.flush()
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.put("加载书籍封面失败\n${e.localizedMessage}", e)
            e.printOnDebug()
        }

    }

    /**
     * 读取PDF文件
     *
     * @return
     */
    private fun readPdf(): PdfRenderer? {
        val uri = book.getLocalUri()
        if (uri.isContentScheme()) {
            fileDescriptor = appCtx.contentResolver.openFileDescriptor(uri, "r")?.also {
                pdfRenderer = PdfRenderer(it)
            }
        } else {
            fileDescriptor =
                ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
                    ?.also {
                        pdfRenderer = PdfRenderer(it)
                    }
        }
        return pdfRenderer
    }

    /**
     * 关闭pdf文件
     *
     */
    private fun closePdf() {
        pdfRenderer?.close()
        fileDescriptor?.close()
    }


    /**
     * 渲染PDF页面
     * 根据index打开pdf页面,并渲染到Bitmap
     *
     * @param renderer
     * @param index
     * @return
     */
    private fun openPdfPage(renderer: PdfRenderer, index: Int): Bitmap? {
        if (index >= renderer.pageCount) {
            return null
        }
        return renderer.openPage(index)?.use { page ->
            Bitmap.createBitmap(
                SystemUtils.screenWidthPx,
                (SystemUtils.screenWidthPx.toDouble() * page.height / page.width).toInt(),
                Bitmap.Config.ARGB_8888
            )
                .apply {
                    this.eraseColor(Color.WHITE)
                    page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
        }

    }

    private fun getContent(chapter: BookChapter): String? =
        if (pdfRenderer == null) {
            null
        } else {
            pdfRenderer?.let { renderer ->

                buildString {
                    val start = chapter.index * PAGE_SIZE
                    val end = ((chapter.index + 1) * PAGE_SIZE).coerceAtMost(renderer.pageCount)
                    (start until end).forEach {
                        append("<img src=").append('"').append(it).append('"').append(" >")
                            .append('\n')
                    }

                }

            }
        }


    private fun getImage(href: String): InputStream? {
        if (pdfRenderer == null) {
            return null
        }
        return try {
            val index = href.toInt()
            val bitmap = openPdfPage(pdfRenderer!!, index)
            if (bitmap != null) {
                BitmapUtils.toInputStream(bitmap)
            } else {
                null
            }

        } catch (e: Exception) {
            return null
        }
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()

        pdfRenderer?.let { renderer ->
            if (renderer.pageCount > 0) {
                val chapterCount = ceil((renderer.pageCount.toDouble() / PAGE_SIZE)).toInt()
                (0 until chapterCount).forEach {
                    val chapter = BookChapter()
                    chapter.index = it
                    chapter.bookUrl = book.bookUrl
                    chapter.title = "分段_${it}"
                    chapter.url = "pdf_${it}"
                    chapterList.add(chapter)
                }
            }
        }
        return chapterList
    }

    private fun upBookInfo() {
        if (pdfRenderer == null) {
            pFile = null
            book.intro = "书籍导入异常"
        } else {
            if (book.name.isEmpty()) {
                book.name = book.originName.replace(".pdf", "")
            }
        }

    }

    protected fun finalize() {
        closePdf()
    }
}