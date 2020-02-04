package io.legado.app.model.localBook

import androidx.documentfile.provider.DocumentFile
import io.legado.app.App
import io.legado.app.data.entities.Book


object LocalBook {

    fun importFile(doc: DocumentFile) {
        doc.name?.let { fileName ->
            val str = fileName.substringBeforeLast(".")
            var name = str.substringBefore("作者")
            val author = str.substringAfter("作者", "")
            val smhStart = name.indexOf("《")
            val smhEnd = name.indexOf("》")
            if (smhStart != -1 && smhEnd != -1) {
                name = (name.substring(smhStart + 1, smhEnd))
            }
            val book = Book(
                bookUrl = doc.uri.toString(),
                name = name,
                author = author,
                originName = fileName
            )
            App.db.bookDao().insert(book)
        }
    }

}