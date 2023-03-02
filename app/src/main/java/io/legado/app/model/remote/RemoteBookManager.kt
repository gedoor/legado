package io.legado.app.model.remote

import android.net.Uri
import io.legado.app.data.entities.Book

abstract class RemoteBookManager {

    /**
     * 获取书籍列表
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBookList(path: String): MutableList<RemoteBook>

    /**
     * 根据书籍地址获取书籍信息
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBook(path: String): RemoteBook?

    /**
     * @return Uri：下载到本地的路径
     */
    @Throws(Exception::class)
    abstract suspend fun downloadRemoteBook(remoteBook: RemoteBook): Uri

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

}