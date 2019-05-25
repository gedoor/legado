package io.legado.app.lib.webdav.http

import okhttp3.OkHttpClient

class OkHttp private constructor() {

    val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private object SingletonHelper {
        val INSTANCE = OkHttp()
    }

    companion object {

        val instance: OkHttp
            get() = SingletonHelper.INSTANCE
    }

}
