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

/**
 * 可在js里调用,source.xxx()
 */
@Suppress("unused")
interface BaseSource : JsExtensions {

    fun getStoreUrl(): String

    var header: String?

    /**
     * 解析header规则
     */
    fun getHeaderMap(hasLoginHeader: Boolean = false) = HashMap<String, String>().apply {
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
        if (hasLoginHeader) {
            getLoginHeaderMap()?.let {
                putAll(it)
            }
        }
    }

    /**
     * 获取用于登录的头部信息
     */
    fun getLoginHeader(): String? {
        return CacheManager.get("loginHeader_${getStoreUrl()}")
    }

    fun getLoginHeaderMap(): Map<String, String>? {
        val cache = getLoginHeader() ?: return null
        return GSON.fromJsonObject(cache)
    }

    /**
     * 保存登录头部信息,map格式,访问时自动添加
     */
    fun putLoginHeader(header: String) {
        CacheManager.put("loginHeader_${getStoreUrl()}", header)
    }

    /**
     * 获取用户信息,可以用来登录
     * 用户信息采用aes加密存储
     */
    fun getLoginInfo(): Map<String, String>? {
        val cache = CacheManager.get("userInfo_${getStoreUrl()}") ?: return null
        val byteArrayB = Base64.decode(cache, Base64.DEFAULT)
        val byteArrayA = EncoderUtils.decryptAES(byteArrayB, AppConst.androidId.toByteArray())
            ?: return null
        val info = String(byteArrayA)
        return GSON.fromJsonObject(info)
    }

    /**
     * 保存用户信息,aes加密
     */
    fun putLoginInfo(info: String) {
        val data = Base64.encodeToString(
            EncoderUtils.decryptAES(
                info.toByteArray(),
                AppConst.androidId.toByteArray()
            ),
            Base64.DEFAULT
        )
        CacheManager.put("userInfo_${getStoreUrl()}", data)
    }

    /**
     * 执行JS
     */
    @Throws(Exception::class)
    fun evalJS(jsStr: String): Any? {
        val bindings = SimpleBindings()
        bindings["java"] = this
        bindings["source"] = this
        bindings["baseUrl"] = getStoreUrl()
        bindings["cookie"] = CookieStore
        bindings["cache"] = CacheManager
        return AppConst.SCRIPT_ENGINE.eval(jsStr, bindings)
    }
}