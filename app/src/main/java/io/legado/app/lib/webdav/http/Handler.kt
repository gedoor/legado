package io.legado.app.lib.webdav.http

import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

class Handler : URLStreamHandler() {

    override fun getDefaultPort(): Int {
        return 80
    }

    public override fun openConnection(u: URL): URLConnection? {
        return null
    }

    override fun parseURL(url: URL, spec: String, start: Int, end: Int) {
        super.parseURL(url, spec, start, end)
    }

    companion object {

        val HANDLER: URLStreamHandler = Handler()
    }
}
