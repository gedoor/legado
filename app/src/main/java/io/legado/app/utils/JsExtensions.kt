@file:Suppress("unused")

package io.legado.app.utils

import org.mozilla.javascript.*

/**
 * 禁止在js里删除文件
 * 参考 https://codeutopia.net/blog/2009/01/02/sandboxing-rhino-in-java/
 */
fun ContextFactory.eval(jsStr: String, scriptObjects: Map<String, Any>): Any {
    val ctx = this.enterContext()
    ctx.optimizationLevel = -1
    ctx.setClassShutter {
        when {
            it.startsWith("java.io") -> false
            else -> true
        }
    }
    val scope: Scriptable = ctx.initStandardObjects()
    for (name in scriptObjects.keys) {
        val obj = scriptObjects[name]
        if (obj is Boolean) {
            scope.put(name, scope, obj)
        } else if (obj != null) {
            val jsArgs = Context.toObject(obj, scope)
            scope.put(name, scope, jsArgs)
        }
    }
    scope.delete("Packages")
    return try {
        ctx.evaluateString(scope, jsStr, null, 1, null)
    } catch (e: RhinoException) {
        e.printStackTrace()
        e.toString()
    } finally {
        Context.exit()
    }
}

class SandboxNativeJavaObject(scope: Scriptable?, javaObject: Any?, staticType: Class<*>?) :
    NativeJavaObject(scope, javaObject, staticType) {
    override fun get(name: String, start: Scriptable): Any {
        return when (name) {
            "getClass", "delete" -> NOT_FOUND
            else -> super.get(name, start)
        }
    }
}

class SandboxWrapFactory : WrapFactory() {
    override fun wrapAsJavaObject(
        cx: Context?,
        scope: Scriptable?,
        javaObject: Any?,
        staticType: Class<*>?
    ): Scriptable {
        return SandboxNativeJavaObject(scope, javaObject, staticType)
    }
}

class SandboxContextFactory : ContextFactory() {
    override fun makeContext(): Context {
        val cx = super.makeContext()
        cx.wrapFactory = SandboxWrapFactory()
        return cx
    }
}