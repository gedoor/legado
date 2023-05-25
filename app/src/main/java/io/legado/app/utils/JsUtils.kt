package io.legado.app.utils

import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.constant.AppPattern.EXP_PATTERN

object JsUtils {

    fun evalJs(js: String, bindingsFun: ((SimpleBindings) -> Unit)? = null): String {
        val bindings = SimpleBindings()
        bindingsFun?.invoke(bindings)
        if (js.contains("{{") && js.contains("}}")) {
            val sb = StringBuffer()
            val expMatcher = EXP_PATTERN.matcher(js)
            while (expMatcher.find()) {
                val result = expMatcher.group(1)?.let { js1 ->
                    RhinoScriptEngine.eval(js1, bindings)
                } ?: ""
                if (result is String) {
                    expMatcher.appendReplacement(sb, result)
                } else if (result is Double && result % 1.0 == 0.0) {
                    expMatcher.appendReplacement(sb, String.format("%.0f", result))
                } else {
                    expMatcher.appendReplacement(sb, result.toString())
                }
            }
            expMatcher.appendTail(sb)
            return sb.toString()
        }
        return RhinoScriptEngine.eval(js, bindings).toString()
    }


}