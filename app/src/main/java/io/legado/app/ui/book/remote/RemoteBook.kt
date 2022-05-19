package io.legado.app.ui.book.remote

data class RemoteBook(
    val filename: String,
    val urlName: String,
    val size: Long,
    val contentType: String,
    val lastModify: Long
)