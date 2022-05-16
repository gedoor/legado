package io.legado.app.ui.book.remote

import android.app.Application
import android.net.Uri
import android.util.Log
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.PreferKey
import io.legado.app.lib.webdav.Authorization
import io.legado.app.lib.webdav.WebDav
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.remote.manager.RemoteBookWebDav
import io.legado.app.utils.FileUtils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getPrefString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import splitties.init.appCtx
import java.io.File
import java.nio.charset.Charset
import java.util.*

class RemoteBookViewModel(application: Application): BaseViewModel(application){
    private val remoteBookFolderName = "book_remote"
    private var dataCallback : DataCallback? = null
    var dataFlowStart: (() -> Unit)? = null
    private var authorization: Authorization? = null


    val dataFlow = callbackFlow<List<RemoteBook>> {

        val list = Collections.synchronizedList(ArrayList<RemoteBook>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<RemoteBook>) {
                list.clear()
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun addItems(remoteFiles: List<RemoteBook>) {
                list.addAll(remoteFiles)
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }
        }
//        withContext(Dispatchers.Main) {
//            dataFlowStart?.invoke()
//        }

        awaitClose {
//            dataCallback = null
        }
    }.flowOn(Dispatchers.IO)

    fun loadRemoteBookList() {
        execute {
            dataCallback?.clear()
            RemoteBookWebDav.getRemoteBookList()
        }
//            dataCallback?.setItems()
        }
//        dataCallback?.setItems(listOf("1", "2", "3"))


    fun downloadRemoteBook(urlName: String) {
        val saveFolder = "${appCtx.externalFiles.absolutePath}${File.separator}${remoteBookFolderName}"
        val trueCodeURLName = String(urlName.toByteArray(Charset.forName("GBK")), Charset.forName("UTF-8"))
        val saveFilePath = "${saveFolder}${trueCodeURLName}"
        execute {
            kotlin.runCatching {
                authorization = null
                val account = appCtx.getPrefString(PreferKey.webDavAccount)
                val password = appCtx.getPrefString(PreferKey.webDavPassword)
                if (!account.isNullOrBlank() && !password.isNullOrBlank()) {
                    val mAuthorization = Authorization(account, password)
                    authorization = mAuthorization
                }
            }

            authorization?.let { it ->
                FileUtils.createFolderIfNotExist(saveFolder).run{
                    withTimeout(15000L) {
                        val webdav = WebDav(
                            "http://txc.qianfanguojin.top${trueCodeURLName}",
                            it
                        )
                        webdav.downloadTo(saveFilePath, true)
                    }
                }
            }
        }.onFinally {
            addToBookshelf(hashSetOf("${saveFolder}${trueCodeURLName}")){
                Log.e("TAG", "downloadRemoteBook: add", )
            }
        }

    }
    fun addToBookshelf(uriList: HashSet<String>, finally: () -> Unit) {
        execute {
            uriList.forEach {
                LocalBook.importFile(Uri.parse(it))
            }
        }.onFinally {
            finally.invoke()
        }
    }
    interface DataCallback {

        fun setItems(remoteFiles: List<RemoteBook>)

        fun addItems(remoteFiles: List<RemoteBook>)

        fun clear()

    }
}

data class RemoteBook(
    val filename: String,
    val urlName: String,
    val size: Long,
    val contentType: String,
    val lastModify: Long
)

