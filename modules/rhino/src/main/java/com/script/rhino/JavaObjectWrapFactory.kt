package com.script.rhino

import org.mozilla.javascript.Scriptable

fun interface JavaObjectWrapFactory {

    fun wrap(scope: Scriptable?, javaObject: Any, staticType: Class<*>?): Scriptable

}
