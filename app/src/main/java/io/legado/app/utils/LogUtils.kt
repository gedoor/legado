@file:Suppress("unused")

package io.legado.app.utils

import android.annotation.SuppressLint
import io.legado.app.BuildConfig
import io.legado.app.help.config.AppConfig
import splitties.init.appCtx
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

@SuppressLint("SimpleDateFormat")
@Suppress("unused")
object LogUtils {
    const val TIME_PATTERN = "yy-MM-dd HH:mm:ss.SSS"
    val logTimeFormat by lazy { SimpleDateFormat(TIME_PATTERN) }

    @JvmStatic
    fun d(tag: String, msg: String) {
        logger.log(Level.INFO, "$tag $msg")
    }

    inline fun d(tag: String, lazyMsg: () -> String) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "$tag ${lazyMsg()}")
        }
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        logger.log(Level.WARNING, "$tag $msg")
    }

    val logger: Logger by lazy {
        Logger.getGlobal().apply {
            fileHandler?.let {
                addHandler(it)
            }
            addHandler(consoleHandler)
        }
    }

    private val fileHandler by lazy {
        val root = appCtx.externalCacheDir ?: return@lazy null
        val logFolder = FileUtils.createFolderIfNotExist(root, "logs")
        val expiredTime = System.currentTimeMillis() - 7.days.inWholeMilliseconds
        logFolder.listFiles()?.forEach {
            if (it.lastModified() < expiredTime) {
                it.delete()
            }
        }
        val date = getCurrentDateStr(TIME_PATTERN)
        val logPath = FileUtils.getPath(root = logFolder, "appLog-$date.txt")
        FileHandler(logPath).apply {
            formatter = object : java.util.logging.Formatter() {
                override fun format(record: LogRecord): String {
                    // 设置文件输出格式
                    return (getCurrentDateStr(TIME_PATTERN) + ": " + record.message + "\n")
                }
            }
            level = if (AppConfig.recordLog) {
                Level.INFO
            } else {
                Level.OFF
            }
        }.asynchronous()
    }

    private val consoleHandler by lazy {
        ConsoleHandler().apply {
            formatter = object : java.util.logging.Formatter() {
                override fun format(record: LogRecord): String {
                    // 设置文件输出格式
                    return (getCurrentDateStr(TIME_PATTERN) + ": " + record.message + "\n")
                }
            }
            level = if (AppConfig.recordLog) {
                Level.INFO
            } else {
                Level.OFF
            }
        }.asynchronous()
    }

    fun upLevel() {
        val level = if (AppConfig.recordLog) {
            Level.INFO
        } else {
            Level.OFF
        }
        fileHandler?.level = level
        consoleHandler.level = level
    }

    /**
     * 获取当前时间
     */
    @SuppressLint("SimpleDateFormat")
    fun getCurrentDateStr(pattern: String): String {
        val date = Date()
        val sdf = SimpleDateFormat(pattern)
        return sdf.format(date)
    }
}

fun Throwable.printOnDebug() {
    if (BuildConfig.DEBUG) {
        printStackTrace()
    }
}
