package io.legado.app.model

import io.legado.app.utils.MD5Utils
import org.mozilla.javascript.Scriptable
import java.lang.ref.WeakReference

object SharedJsScope {

    private val scopeMap = hashMapOf<String, WeakReference<Scriptable>>()

    fun getScope(jsLib: String) {
        val key = MD5Utils.md5Encode(jsLib)
        var scope = scopeMap[key]?.get()
        if (scope == null) {

        }
    }

}