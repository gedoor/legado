package io.legado.app.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.Wrapper

object Rhino {

    inline fun <T> use(block: Context.(Scriptable) -> T): T {
        return try {
            val cx = Context.enter()
            val scope = cx.initStandardObjects(ImporterTopLevel(cx))
            block.invoke(cx, scope)
        } finally {
            Context.exit()
        }
    }

    fun unwrapReturnValue(result: Any?): Any? {
        var result1 = result
        if (result1 is Wrapper) {
            result1 = result1.unwrap()
        }
        return if (result1 is Undefined) null else result1
    }

//    init {
//        ContextFactory.initGlobal(object : RhinoContextFactory() {
//
//            override fun makeContext(): Context {
//                val cx = super.makeContext()
//                cx.languageVersion = 200
//                cx.optimizationLevel = -1
//                cx.setClassShutter(RhinoClassShutter)
//                return cx
//            }
//
//        })
//    }

}