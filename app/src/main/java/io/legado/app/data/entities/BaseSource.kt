package io.legado.app.data.entities

import android.util.Base64
import io.legado.app.constant.AppConst
import io.legado.app.help.AppConfig
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.EncoderUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import javax.script.SimpleBindings

interface BaseSource : JsExtensions {

    fun getStoreUrl(): String

    var header: String?

    fun getHeaderMap() = HashMap<String, String>().apply {
        this[AppConst.UA_NAME] = AppConfig.userAgent
        header?.let {
            GSON.fromJsonObject<Map<String, String>>(
                when {
                    it.startsWith("@js:", true) ->
                        evalJS(it.substring(4)).toString()
                    it.startsWith("<js>", true) ->
                        evalJS(it.substring(4, it.lastIndexOf("<"))).toString()
                    else -> it
                }
            )?.let { map ->
                putAll(map)
            }
        }
    }

    fun getLoginHeader(): Map<String, String>? {
        val cache = CacheManager.get("login_${getStoreUrl()}") ?: return null
        val byteArrayB = Base64.decode(cache, Base64.DEFAULT)
        val byteArrayA = EncoderUtils.decryptAES(byteArrayB, AppConst.androidId.toByteArray())
            ?: return null
        val headerStr = String(byteArrayA)
        return GSON.fromJsonObject(headerStr)
    }

    fun putLoginHeader(header: String) {
        val data = Base64.encodeToString(
            EncoderUtils.decryptAES(
                header.toByteArray(),
                AppConst.androidId.toByteArray()
            ),
            Base64.DEFAULT
        )
        CacheManager.put("login_${getStoreUrl()}", data)
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    private fun evalJS(jsStr: String): Any? {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["cookie"] = CookieStore
        bindings["cache"] = CacheManager
        return AppConst.SCRIPT_ENGINE.eval(jsStr, bindings)
    }
}