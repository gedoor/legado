package io.legado.app.utils

import org.mozilla.javascript.Context

private val enterCountField by lazy {
    Context::class.java.getDeclaredField("enterCount").apply {
        isAccessible = true
    }
}

fun Context?.getEnterCount(): Int {
    this ?: return 0
    return enterCountField.get(this) as Int
}

fun checkRhinoRecursiveCall(): Boolean {
    return Context.getCurrentContext().getEnterCount() > 1
}

fun checkRhinoCall(): Boolean {
    return Context.getCurrentContext() != null
}
