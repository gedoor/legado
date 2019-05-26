package io.legado.app.lib.webdav.http

import okhttp3.OkHttpClient

class OkHttp private constructor() {

    object SingletonHelper {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()
    }

}
