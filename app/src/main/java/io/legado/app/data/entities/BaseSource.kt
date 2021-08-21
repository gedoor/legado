package io.legado.app.data.entities

import io.legado.app.constant.AppConst
import io.legado.app.help.AppConfig
import io.legado.app.help.CacheManager
import io.legado.app.help.JsExtensions
import io.legado.app.help.http.CookieStore
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import java.util.*
import javax.script.SimpleBindings

interface BaseSource : JsExtensions {

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