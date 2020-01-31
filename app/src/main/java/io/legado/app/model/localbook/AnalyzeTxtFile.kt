package io.legado.app.model.localbook

import android.content.Context
import android.net.Uri
import io.legado.app.data.entities.Book
import io.legado.app.utils.EncodingDetect

object AnalyzeTxtFile {


    fun analyze(context: Context, book: Book) {
        context.contentResolver.openInputStream(Uri.parse(book.bookUrl))?.use { stream ->
            val rawByteArray = ByteArray(2000)
            stream.read(rawByteArray)
            book.charset = EncodingDetect.getJavaEncode(rawByteArray)


        }
    }

}