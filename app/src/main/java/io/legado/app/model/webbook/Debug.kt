package io.legado.app.model.webbook

import android.annotation.SuppressLint
import io.legado.app.utils.htmlFormat
import java.text.SimpleDateFormat
import java.util.*

object Debug {

    var debugSource: String? = null
    var callback: Callback? = null
    @SuppressLint("ConstantLocale")
    private val DEBUG_TIME_FORMAT = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())
    private val startTime: Long = System.currentTimeMillis()

    interface Callback {
        fun printLog(state: Int, msg: String)
    }

    fun printLog(source: String, state: Int, msg: String, print: Boolean = true, isHtml: Boolean = false) {
        if (debugSource != source) return
        if (!print) return
        var printMsg = msg
        if (isHtml) {
            printMsg = printMsg.htmlFormat()
        }
        printMsg =
            String.format("%s %s", DEBUG_TIME_FORMAT.format(Date(System.currentTimeMillis() - startTime)), printMsg)
        callback?.printLog(state, printMsg)
    }

}