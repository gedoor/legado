package io.legado.app.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

object TimeUtils {

    private val formatArray = arrayOf("")

    @SuppressLint("SimpleDateFormat")
    fun stringTimeToLong(time: String): Long {

        for (str in formatArray) {
            try {
                val df = SimpleDateFormat(str)
                val d = df.parse(time)
                return d.time
            } catch (e: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

}