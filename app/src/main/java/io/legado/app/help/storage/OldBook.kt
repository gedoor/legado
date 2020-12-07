package io.legado.app.help.storage

import android.util.Log
import io.legado.app.App
import io.legado.app.constant.AppConst
import io.legado.app.data.entities.Book
import io.legado.app.utils.readBool
import io.legado.app.utils.readInt
import io.legado.app.utils.readLong
import io.legado.app.utils.readString

object OldBook {

    fun toNewBook(json: String): List<Book> {
        val books = mutableListOf<Book>()
        val items: List<Map<String, Any>> = Restore.jsonPath.parse(json).read("$")
        val existingBooks = App.db.bookDao.allBookUrls.toSet()
        for (item in items) {
            val jsonItem = Restore.jsonPath.parse(item)
            val book = Book()
            book.bookUrl = jsonItem.readString("$.noteUrl") ?: ""
            if (book.bookUrl.isBlank()) continue
            book.name = jsonItem.readString("$.bookInfoBean.name") ?: ""
            if (book.bookUrl in existingBooks) {
                Log.d(AppConst.APP_TAG, "Found existing book: ${book.name}")
                continue
            }
            book.origin = jsonItem.readString("$.tag") ?: ""
            book.originName = jsonItem.readString("$.bookInfoBean.origin") ?: ""
            book.author = jsonItem.readString("$.bookInfoBean.author") ?: ""
            book.type =
                if (jsonItem.readString("$.bookInfoBean.bookSourceType") == "AUDIO") 1 else 0
            book.tocUrl = jsonItem.readString("$.bookInfoBean.chapterUrl") ?: book.bookUrl
            book.coverUrl = jsonItem.readString("$.bookInfoBean.coverUrl")
            book.customCoverUrl = jsonItem.readString("$.customCoverPath")
            book.lastCheckTime = jsonItem.readLong("$.bookInfoBean.finalRefreshData") ?: 0
            book.canUpdate = jsonItem.readBool("$.allowUpdate") == true
            book.totalChapterNum = jsonItem.readInt("$.chapterListSize") ?: 0
            book.durChapterIndex = jsonItem.readInt("$.durChapter") ?: 0
            book.durChapterTitle = jsonItem.readString("$.durChapterName")
            book.durChapterPos = jsonItem.readInt("$.durChapterPage") ?: 0
            book.durChapterTime = jsonItem.readLong("$.finalDate") ?: 0
            book.intro = jsonItem.readString("$.bookInfoBean.introduce")
            book.latestChapterTitle = jsonItem.readString("$.lastChapterName")
            book.lastCheckCount = jsonItem.readInt("$.newChapters") ?: 0
            book.order = jsonItem.readInt("$.serialNumber") ?: 0
            book.variable = jsonItem.readString("$.variable")
            book.setUseReplaceRule(jsonItem.readBool("$.useReplaceRule") == true)
            books.add(book)
        }
        return books
    }

}