package io.legado.app.model

import com.script.SimpleBindings
import io.legado.app.constant.SCRIPT_ENGINE
import io.legado.app.help.rhino.RhinoScriptEngine
import io.legado.app.utils.MD5Utils
import org.mozilla.javascript.Scriptable
import java.lang.ref.WeakReference

object SharedJsScope {

    private val scopeMap = hashMapOf<String, WeakReference<Scriptable>>()

    fun getScope(jsLib: String): Scriptable {
        val key = MD5Utils.md5Encode(jsLib)
        var scope = scopeMap[key]?.get()
        if (scope == null) {
            val context = SCRIPT_ENGINE.getScriptContext(SimpleBindings())
            scope = SCRIPT_ENGINE.getRuntimeScope(context)
            RhinoScriptEngine.run {
                it.evaluateString(scope, jsLib, "jsLib", 1, null)
            }
            scopeMap[key] = WeakReference(scope)
        }
        return scope
    }

}