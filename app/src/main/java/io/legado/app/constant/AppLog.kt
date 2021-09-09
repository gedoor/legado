package io.legado.app.constant

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

object AppLog {

    @SuppressLint("ConstantLocale")
    private val logTimeFormat = SimpleDateFormat("[mm:ss.SSS]", Locale.getDefault())

    val logs = arrayListOf<String>()

    fun addLog(log: String?) {
        log ?: return
        synchronized(logs) {
            if (logs.size > 1000) {
                logs.removeLastOrNull()
            }
            logs.add(0, logTimeFormat.format(Date()) + " " + log)
        }
    }

}