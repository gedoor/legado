package io.legado.app.help

import com.script.SimpleBindings
import com.script.rhino.RhinoScriptEngine
import io.legado.app.data.entities.BaseSource

object JsEngine : JsExtensions {

    override fun getSource(): BaseSource? {
        return null
    }

    fun eval(js: String): Any? {
        val bindings = SimpleBindings()
        bindings["java"] = this
        return RhinoScriptEngine.eval(js, bindings)
    }

}