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

fun checkRhinoRecursiveCall() {
    check(Context.getCurrentContext().getEnterCount() <= 1) {
        "Rhino recursive call detected."
    }
}

fun checkRhinoCall() {
    check(Context.getCurrentContext() == null) {
        "Rhino call detected."
    }
}
