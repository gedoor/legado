package io.legado.app.help.http.cronet

import okhttp3.Interceptor

object Cronet {

    val loader: CronetLoaderInterface? by lazy {
        val cl = Class.forName("io.legado.app.lib.cronet.CronetLoader")
            ?.kotlin?.objectInstance
        cl as? CronetLoaderInterface
    }

    fun preDownload() {
        loader?.preDownload()
    }

    val interceptor: Interceptor? by lazy {
        val cl = Class.forName("io.legado.app.lib.cronet.CronetInterceptor")?.newInstance()
        cl as? Interceptor
    }

}