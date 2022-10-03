package io.legado.app.model.remote

import android.net.Uri
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.RemoteBook

abstract class RemoteBookManager {
    protected val remoteBookFolder: String = "books"

    abstract suspend fun initRemoteContext()

    /**
     * 获取书籍列表
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBookList(path: String): MutableList<RemoteBook>

    /**
     * 上传书籍
     */
    @Throws(Exception::class)
    abstract suspend fun upload(book: Book)

    /**
     * 删除书籍
     */
    @Throws(Exception::class)
    abstract suspend fun delete(remoteBookUrl: String)

    /**
     * @return Uri：下载到本地的路径
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBook(remoteBook: RemoteBook): Uri
}