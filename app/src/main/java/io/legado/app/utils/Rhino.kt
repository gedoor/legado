package io.legado.app.utils

import org.mozilla.javascript.Context

object Rhino {

    inline fun <R> use(block: Context.() -> R): R {
        return try {
            block.invoke(Context.enter())
        } finally {
            Context.exit()
        }
    }

}