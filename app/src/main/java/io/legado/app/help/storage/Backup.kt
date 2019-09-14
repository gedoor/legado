package io.legado.app.help.storage

import io.legado.app.App
import io.legado.app.utils.GSON

object Backup {

    fun backup() {

    }

    fun autoBackup() {

    }

    private fun backupBookshelf() {
        val books = App.db.bookDao().allBooks
        val json = GSON.toJson(books)

    }
}