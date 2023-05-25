package io.legado.app.web.socket


import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.model.Debug
import io.legado.app.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import splitties.init.appCtx

import java.io.IOException

/**
 * web端书源调试
 */
class BookSourceDebugWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession) :
    NanoWSD.WebSocket(handshakeRequest),
    CoroutineScope by MainScope(),
    Debug.Callback {

    private val notPrintState = arrayOf(10, 20, 30, 40)

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
                val debugBean =
                    GSON.fromJsonObject<Map<String, String>>(message.textPayload).getOrNull()
                if (debugBean != null) {
                    val tag = debugBean["tag"]
                    val key = debugBean["key"]
                    if (tag.isNullOrBlank() || key.isNullOrBlank()) {
                        send(appCtx.getString(R.string.cannot_empty))
                        close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                        return@launch
                    }
                    appDb.bookSourceDao.getBookSource(tag)?.let {
                        Debug.callback = this@BookSourceDebugWebSocket
                        Debug.startDebug(this, it, key)
                    }
                } else {
                    send("数据必须为Json格式")
                    close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                    return@launch
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
        if (state in notPrintState) {
            return
        }
        runOnIO {
            runCatching {
                send(msg)
                if (state == -1 || state == 1000) {
                    Debug.cancelDebug(true)
                    close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "调试结束", false)
                }
            }.onFailure {
                it.printOnDebug()
            }
        }
    }

}
