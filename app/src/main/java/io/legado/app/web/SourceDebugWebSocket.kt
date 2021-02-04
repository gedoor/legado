package io.legado.app.web


import android.os.Looper
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.legado.app.App
import io.legado.app.R
import io.legado.app.model.Debug
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.isJson
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import splitties.init.appCtx
import java.io.IOException


class SourceDebugWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession) :
    NanoWSD.WebSocket(handshakeRequest),
    CoroutineScope by MainScope(),
    Debug.Callback {

    override fun onOpen() {
        launch(IO) {
            kotlin.runCatching {
                while (isOpen) {
                    ping("ping".toByteArray())
                    delay(30000)
                }
            }
        }
    }

    override fun onClose(
        code: NanoWSD.WebSocketFrame.CloseCode,
        reason: String,
        initiatedByRemote: Boolean
    ) {
        cancel()
        Debug.cancelDebug(true)
    }

    override fun onMessage(message: NanoWSD.WebSocketFrame) {
        launch(IO) {
            kotlin.runCatching {
                if (!message.textPayload.isJson()) {
                    send("数据必须为Json格式")
                    close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                    return@launch
                }
                val debugBean = GSON.fromJsonObject<Map<String, String>>(message.textPayload)
                if (debugBean != null) {
                    val tag = debugBean["tag"]
                    val key = debugBean["key"]
                    if (tag.isNullOrBlank() || key.isNullOrBlank()) {
                        send(appCtx.getString(R.string.cannot_empty))
                        close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                        return@launch
                    }
                    App.db.bookSourceDao.getBookSource(tag)?.let {
                        Debug.callback = this@SourceDebugWebSocket
                        Debug.startDebug(this, WebBook(it), key)
                    }
                }
            }
        }
    }

    override fun onPong(pong: NanoWSD.WebSocketFrame) {

    }

    override fun onException(exception: IOException) {
        Debug.cancelDebug(true)
    }

    override fun printLog(state: Int, msg: String) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            launch(IO) {
                runCatching {
                    send(msg)
                    if (state == -1 || state == 1000) {
                        Debug.cancelDebug(true)
                        close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        } else {
            runCatching {
                send(msg)
                if (state == -1 || state == 1000) {
                    Debug.cancelDebug(true)
                    close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                }
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

}
