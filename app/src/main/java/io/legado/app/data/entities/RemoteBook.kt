package io.legado.app.data.entities

import com.android.tools.r8.Keep

@Keep
data class RemoteBook(
    val filename: String,
    val path: String,
    val size: Long,
    val contentType: String,
    val lastModify: Long,
    var isOnBookShelf: Boolean = false
) {

    val isDir by lazy {
        contentType == "folder"
    }

}