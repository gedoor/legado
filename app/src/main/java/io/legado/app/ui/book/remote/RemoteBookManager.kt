package io.legado.app.ui.book.remote

import android.net.Uri

abstract class RemoteBookManager {
    protected val remoteBookFolder : String = "books"
    abstract suspend fun initRemoteContext()
    abstract suspend fun getRemoteBookList(path: String): MutableList<RemoteBook>
    abstract suspend fun upload(localBookUri: Uri): Boolean
    abstract suspend fun delete(remoteBookUrl: String): Boolean

    /**
     * @return String：下载到本地的路径
     */
    abstract suspend fun getRemoteBook(remoteBook: RemoteBook): Uri?
}