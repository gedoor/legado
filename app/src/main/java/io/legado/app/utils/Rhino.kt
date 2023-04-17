package io.legado.app.utils

import org.mozilla.javascript.Context

object Rhino {

    inline fun <R> use(block: (Context) -> R): R {
        val context = Context.enter()
        return try {
            block.invoke(context)
        } finally {
            Context.exit()
        }
    }

}