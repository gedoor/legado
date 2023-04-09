package io.legado.app.help.rhino

import org.mozilla.javascript.Context

object RhinoScriptEngine {


    fun run(function: (Context) -> Any?): Any? {
        return try {
            val context = Context.enter()
            function.invoke(context)
        } finally {
            Context.exit()
        }
    }


}