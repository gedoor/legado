package io.legado.app.help.http.cronet

import okhttp3.Interceptor

object Cronet {

    val loader: CronetLoaderInterface? by lazy {
        kotlin.runCatching {
            Class.forName("io.legado.app.lib.cronet.CronetLoader")
                .kotlin.objectInstance as CronetLoaderInterface
        }.getOrNull()
    }

    fun preDownload() {
        loader?.preDownload()
    }

    val interceptor: Interceptor? by lazy {
        kotlin.runCatching {
            Class.forName("io.legado.app.lib.cronet.CronetInterceptor")
                .newInstance() as Interceptor
        }.getOrNull()
    }

}