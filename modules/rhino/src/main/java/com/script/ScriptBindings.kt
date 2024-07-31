package com.script

import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject

class ScriptBindings : NativeObject() {

    companion object {
        private val topLevelScope: ScriptableObject by lazy {
            val cx = Context.enter()
            try {
                cx.initStandardObjects()
            } finally {
                Context.exit()
            }
        }
    }

    init {
        prototype = topLevelScope
    }

    operator fun set(key: String, value: Any?) {
        Context.enter()
        try {
            put(key, this, Context.javaToJS(value, this))
        } finally {
            Context.exit()
        }
    }

    operator fun set(index: Int, value: Any?) {
        Context.enter()
        try {
            put(index, this, Context.javaToJS(value, this))
        } finally {
            Context.exit()
        }
    }

    fun put(key: String, value: Any?) {
        set(key, value)
    }

}
