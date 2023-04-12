package io.legado.app.model

import com.google.gson.reflect.TypeToken
import com.script.SimpleBindings
import io.legado.app.constant.SCRIPT_ENGINE
import io.legado.app.help.rhino.RhinoScriptEngine
import io.legado.app.utils.GSON
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.isJsonObject
import org.mozilla.javascript.Scriptable
import java.lang.ref.WeakReference
import kotlin.collections.Map
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.set

object SharedJsScope {

    private val scopeMap = hashMapOf<String, WeakReference<Scriptable>>()

    fun getScope(jsLib: String?): Scriptable? {
        if (jsLib.isNullOrBlank()) {
            return null
        }
        val key = MD5Utils.md5Encode(jsLib)
        var scope = scopeMap[key]?.get()
        if (scope == null) {
            val context = SCRIPT_ENGINE.getScriptContext(SimpleBindings())
            scope = SCRIPT_ENGINE.getRuntimeScope(context)
            RhinoScriptEngine.run {
                if (jsLib.isJsonObject()) {
                    val jsMap: Map<String, String> = GSON.fromJson(
                        jsLib,
                        TypeToken.getParameterized(
                            Map::class.java,
                            String::class.java,
                            String::class.java
                        ).type
                    )
                    jsMap.values.forEach { url ->

                    }
                } else {
                    it.evaluateString(scope, jsLib, "jsLib", 1, null)
                }
            }
            scopeMap[key] = WeakReference(scope)
        }
        return scope
    }

}