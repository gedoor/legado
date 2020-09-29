package io.legado.app.web

import fi.iki.elonen.NanoWSD
import io.legado.app.web.utils.SourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        return if (handshake.uri == "/sourceDebug") {
            SourceDebugWebSocket(handshake)
        } else null
    }
}
