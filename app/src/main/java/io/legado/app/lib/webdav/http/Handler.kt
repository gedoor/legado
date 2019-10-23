package io.legado.app.lib.webdav.http

import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

object Handler : URLStreamHandler() {

    override fun getDefaultPort(): Int {
        return 80
    }

    public override fun openConnection(u: URL): URLConnection? {
        return null
    }
}
