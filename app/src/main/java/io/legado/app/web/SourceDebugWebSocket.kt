package io.legado.app.web


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
import java.io.IOException


class SourceDebugWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession) :
    NanoWSD.WebSocket(handshakeRequest),
    CoroutineScope by MainScope(),
    Debug.Callback {

    override fun onOpen() {
        launch(IO) {
            do {
                delay(30000)
                runCatching {
                    ping("ping".toByteArray())
                }
            } while (isOpen)
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
        if (!message.textPayload.isJson()) return
        kotlin.runCatching {
            val debugBean = GSON.fromJsonObject<Map<String, String>>(message.textPayload)
            if (debugBean != null) {
                val tag = debugBean["tag"]
                val key = debugBean["key"]
                if (tag.isNullOrBlank() || key.isNullOrBlank()) {
                    kotlin.runCatching {
                        send(App.INSTANCE.getString(R.string.cannot_empty))
                        close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                    }
                    return
                }
                App.db.bookSourceDao.getBookSource(tag)?.let {
                    Debug.callback = this
                    Debug.startDebug(this, WebBook(it), key)
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
        kotlin.runCatching {
            send(msg)
            if (state == -1 || state == 1000) {
                Debug.cancelDebug(true)
            }
        }
    }

}
