package io.legado.app.lib.webdav.http

import okhttp3.OkHttpClient

object OkHttp {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()
}