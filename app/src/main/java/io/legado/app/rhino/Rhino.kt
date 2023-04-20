package io.legado.app.rhino

import com.script.RhinoContextFactory
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.Wrapper

object Rhino {

    inline fun use(block: Context.() -> Any?): Any? {
        return try {
            val cx = Context.enter()
            val result = block.invoke(cx)
            unwrapReturnValue(result)
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

    init {
        ContextFactory.initGlobal(object : RhinoContextFactory() {

            override fun makeContext(): Context {
                val cx = super.makeContext()
                cx.languageVersion = 200
                cx.optimizationLevel = -1
                cx.setClassShutter(RhinoClassShutter)
                //cx.wrapFactory = RhinoWrapFactory
                return cx
            }

        })
    }

}