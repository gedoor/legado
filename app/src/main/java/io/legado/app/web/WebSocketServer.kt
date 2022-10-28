package io.legado.app.web

import fi.iki.elonen.NanoWSD
import io.legado.app.service.WebService
import io.legado.app.web.socket.BookSourceDebugWebSocket
import io.legado.app.web.socket.RssSourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        WebService.serve()
        return when (handshake.uri) {
            "/bookSourceDebug" -> {
                BookSourceDebugWebSocket(handshake)
            }
            "/rssSourceDebug" -> {
                RssSourceDebugWebSocket(handshake)
            }
            else -> null
        }
    }
}
