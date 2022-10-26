package io.legado.app.utils

import kotlin.math.abs

fun Long.toTimeAgo(): String {
    val curTime = System.currentTimeMillis()
    val time = this
    val seconds = abs(System.currentTimeMillis() - time) / 1000f
    val end = if (time < curTime) "前" else "后"

    val start = when {
        seconds < 60 -> "${seconds.toInt()}秒"
        seconds < 3600 -> {
            val minutes = seconds / 60f
            "${minutes.toInt()}分钟"
        }
        seconds < 86400 -> {
            val hours = seconds / 3600f
            "${hours.toInt()}小时"
        }
        seconds < 604800 -> {
            val days = seconds / 86400f
            "${days.toInt()}天"
        }
        seconds < 2_628_000 -> {
            val weeks = seconds / 604800f
            "${weeks.toInt()}周"
        }
        seconds < 31_536_000 -> {
            val months = seconds / 2_628_000f
            "${months.toInt()}月"
        }
        else -> {
            val years = seconds / 31_536_000f
            "${years.toInt()}年"
        }
    }
    return start + end
}