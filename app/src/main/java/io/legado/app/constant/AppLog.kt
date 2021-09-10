package io.legado.app.constant

object AppLog {

    val logs = arrayListOf<Triple<Long, String, Throwable?>>()

    @Synchronized
    fun addLog(message: String?, throwable: Throwable? = null) {
        message ?: return
        if (logs.size > 100) {
            logs.removeLastOrNull()
        }
        logs.add(0, Triple(System.currentTimeMillis(), message, throwable))
    }

}