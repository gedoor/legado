package io.legado.app.web.utils

import android.content.res.AssetManager
import android.text.TextUtils
import fi.iki.elonen.NanoHTTPD
import splitties.init.appCtx
import java.io.File
import java.io.IOException


class AssetsWeb(rootPath: String) {
    private val assetManager: AssetManager = appCtx.assets
    private var rootPath = "web"

    init {
        if (!TextUtils.isEmpty(rootPath)) {
            this.rootPath = rootPath
        }
    }

    @Throws(IOException::class)
    fun getResponse(path: String): NanoHTTPD.Response {
        var path1 = path
        path1 = (rootPath + path1).replace("/+".toRegex(), File.separator)
        val inputStream = assetManager.open(path1)
        return NanoHTTPD.newChunkedResponse(
            NanoHTTPD.Response.Status.OK,
            getMimeType(path1),
            inputStream
        )
    }

    private fun getMimeType(path: String): String {
        val suffix = path.substring(path.lastIndexOf("."))
        return when {
            suffix.equals(".html", ignoreCase = true)
                    || suffix.equals(".htm", ignoreCase = true) -> "text/html"
            suffix.equals(".js", ignoreCase = true) -> "text/javascript"
            suffix.equals(".css", ignoreCase = true) -> "text/css"
            suffix.equals(".ico", ignoreCase = true) -> "image/x-icon"
            suffix.equals(".jpg", ignoreCase = true) -> "image/jpg"
            else -> "text/html"
        }
    }
}
