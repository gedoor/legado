package io.legado.app.ui.book.remote

import android.app.Application
import android.util.Log
import android.widget.Toast
import io.legado.app.base.BaseViewModel
import io.legado.app.utils.FileDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.reflect.typeOf

class RemoteBookViewModel(application: Application): BaseViewModel(application){

    private var dataCallback : DataCallback? = null
    var dataFlowStart: (() -> Unit)? = null
    val dataFlow = callbackFlow<List<String>> {

        val list = Collections.synchronizedList(ArrayList<String>())

        dataCallback = object : DataCallback {

            override fun setItems(remoteFiles: List<String>) {
                list.clear()
                list.addAll(remoteFiles)
                Log.e("TAG", ": 1", )
                trySend(list)
            }

            override fun addItems(remoteFiles: List<String>) {
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
            dataCallback = null
        }
    }.flowOn(Dispatchers.Main)

    fun loadRemoteBookList() {
        Log.e("TAG", dataCallback.toString(), )
        dataCallback?.setItems(listOf("1", "2", "3"))
    }

    interface DataCallback {

        fun setItems(remoteFiles: List<String>)

        fun addItems(remoteFiles: List<String>)

        fun clear()

    }
}