package io.legado.app.utils

import android.annotation.SuppressLint
import io.legado.app.App
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter

@Suppress("unused")
object LogUtils {
    const val TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"

    @JvmStatic
    fun d(tag: String, msg: String) {
        logger.log(Level.INFO, "$tag $msg")
    }

    @JvmStatic
    fun e(tag: String, msg: String) {
        logger.log(Level.WARNING, "$tag $msg")
    }

    private val logger: Logger by lazy {
        Logger.getGlobal().apply {
            fileHandler?.let {
                addHandler(it)
            }
        }
    }

    private val fileHandler by lazy {
        val root = App.INSTANCE.externalCacheDir ?: return@lazy null
        val logFolder = FileUtils.createFolderIfNotExist(root, "logs")
        val logPath = FileUtils.getPath(root = logFolder, "appLog")
        FileHandler(logPath, 10240, 10).apply {
            formatter = object : Formatter() {
                override fun format(record: LogRecord): String {
                    // 设置文件输出格式
                    return (getCurrentDateStr(TIME_PATTERN) + ": " + record.message + "\n")
                }
            }
            level = if (App.INSTANCE.getPrefBoolean("recordLog")) {
                Level.INFO
            } else {
                Level.OFF
            }
        }
    }

    fun upLevel() {
        fileHandler?.level = if (App.INSTANCE.getPrefBoolean("recordLog")) {
            Level.INFO
        } else {
            Level.OFF
        }
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