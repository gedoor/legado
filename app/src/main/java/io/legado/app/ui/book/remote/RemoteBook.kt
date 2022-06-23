package io.legado.app.ui.book.remote

data class RemoteBook(
    val filename: String,
    val path: String,
    val size: Long,
    val contentType: String,
    val lastModify: Long,
    val isOnBookShelf: Boolean
) {

    val isDir by lazy {
        contentType == "folder"
    }

}