@file:Suppress("unused")

package io.legado.app.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.io.Reader

fun Context.evaluateString(scope: Scriptable, source: String, sourceName: String) {
    evaluateString(scope, source, sourceName, 1, null)
}

fun Context.evaluateReader(scope: Scriptable, reader: Reader, sourceName: String) {
    evaluateReader(scope, reader, sourceName, 1, null)
}

fun Scriptable.putBinding(key: String, value: Any?) {
    val wrappedOut = Context.javaToJS(value, this)
    ScriptableObject.putProperty(this, key, wrappedOut)
}

fun Scriptable.putBindings(bindings: Map<String, Any?>) {
    bindings.forEach { (t, u) ->
        putBinding(t, u)
    }
}