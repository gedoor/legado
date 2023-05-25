@file:Suppress("unused")

package io.legado.app.rhino

import com.script.Bindings
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.Reader

fun Context.eval(
    scope: Scriptable,
    source: String,
    sourceName: String = "<Unknown source>",
    lineno: Int = 1,
    securityDomain: Any? = null
): Any? {
    return evaluateString(scope, source, sourceName, lineno, securityDomain)
}

fun Context.eval(
    scope: Scriptable,
    reader: Reader,
    sourceName: String = "<Unknown source>",
    lineno: Int = 1,
    securityDomain: Any? = null
): Any? {
    return evaluateReader(scope, reader, sourceName, lineno, securityDomain)
}

fun Scriptable.putBinding(key: String, value: Any?) {
    val wrappedOut = Context.javaToJS(value, this)
    ScriptableObject.putProperty(this, key, wrappedOut)
}

fun Scriptable.putBindings(bindings: Bindings) {
    bindings.forEach { (t, u) ->
        putBinding(t, u)
    }
}