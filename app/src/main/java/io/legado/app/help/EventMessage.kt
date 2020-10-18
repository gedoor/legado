package io.legado.app.help

import android.text.TextUtils

@Suppress("unused")
class EventMessage {

    var what: Int?=null
    var tag: String? = null
    var obj: Any? = null

    fun isFrom(tag: String): Boolean {
        return TextUtils.equals(this.tag, tag)
    }

    fun maybeFrom(vararg tags: String): Boolean {
        return listOf(*tags).contains(tag)
    }

    companion object {

        fun obtain(tag: String): EventMessage {
            val message = EventMessage()
            message.tag = tag
            return message
        }

        fun obtain(what: Int): EventMessage {
            val message = EventMessage()
            message.what = what
            return message
        }

        fun obtain(what: Int, obj: Any): EventMessage {
            val message = EventMessage()
            message.what = what
            message.obj = obj
            return message
        }

        fun obtain(tag: String, obj: Any): EventMessage {
            val message = EventMessage()
            message.tag = tag
            message.obj = obj
            return message
        }
    }

}
