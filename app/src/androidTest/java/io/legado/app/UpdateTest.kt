package io.legado.app

import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.http.okHttpClient
import io.legado.app.utils.channel
import io.legado.app.utils.jsonPath
import okhttp3.Request
import org.junit.Test
import splitties.init.appCtx

class UpdateTest {

    private val lastReleaseUrl = "https://api.github.com/repos/gedoor/legado/releases/latest"

    @Test
    fun updateApp() {
        val body = okHttpClient.newCall(Request.Builder().url(lastReleaseUrl).build()).execute()
            .body!!.string()
        val rootDoc = jsonPath.parse(body)
        val downloadUrl =
            rootDoc.read<List<String>>("\$.assets[?(@.name =~ /legado_app_.*?apk\$/)].browser_download_url")
        print(downloadUrl)
    }

    @Test
    fun updateLollipop() {
        val body = okHttpClient.newCall(Request.Builder().url(lastReleaseUrl).build()).execute()
            .body!!.string()
        val rootDoc = jsonPath.parse(body)
        val downloadUrl =
            rootDoc.read<List<String>>("\$.assets[?(@.name =~ /legado_lollipop_.*?apk\$/)].browser_download_url")
        print(downloadUrl)
    }

    @Test
    fun updateChannel() {
        val body = okHttpClient.newCall(Request.Builder().url(lastReleaseUrl).build()).execute()
            .body!!.string()
        val rootDoc = jsonPath.parse(body)
        val path = "\$.assets[?(@.name =~ /legado_${appCtx.channel}_.*?apk\$/)]"
        val downloadUrl = rootDoc.read<List<String>>("${path}.browser_download_url")
            .firstOrNull()
            ?: throw NoStackTraceException("获取新版本出错")
        val fileName = rootDoc.read<List<String>>("${path}.name")
            .firstOrNull()
            ?: throw NoStackTraceException("获取新版本出错")
        print(downloadUrl)
        print(fileName)
    }

}