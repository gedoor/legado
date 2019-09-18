package io.legado.app.utils

import android.annotation.SuppressLint
import io.legado.app.App
import io.legado.app.help.FileHelp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter


object LogUtils {

    private val logger: Logger by lazy {
        Logger.getGlobal().apply {
            addFileHandler(
                this,
                Level.INFO,
                FileHelp.getCachePath()
            )
        }
    }

    fun log(msg: String) {
        if (App.INSTANCE.getPrefBoolean("recordLog")) {
            logger.log(Level.INFO, msg)
        }
    }

    private const val DATE_PATTERN = "yyyy-MM-dd"
    const val TIME_PATTERN = "HH:mm:ss"

    /**
     * 为log添加控制台handler
     *
     * @param log 要添加handler的log
     * @param level 控制台的输出等级
     */
    fun addConsoleHandler(log: Logger, level: Level) {
        // 控制台输出的handler
        val consoleHandler = ConsoleHandler()
        // 设置控制台输出的等级（如果ConsoleHandler的等级高于或者等于log的level，则按照FileHandler的level输出到控制台，如果低于，则按照Log等级输出）
        consoleHandler.level = level
        // 添加控制台的handler
        log.addHandler(consoleHandler)
    }

    /**
     * 为log添加文件输出Handler
     *
     * @param log 要添加文件输出handler的log
     * @param level log输出等级
     * @param filePath 指定文件全路径
     */
    fun addFileHandler(log: Logger, level: Level, filePath: String) {
        var fileHandler: FileHandler? = null
        try {
            fileHandler =
                FileHandler(filePath + File.separator + getCurrentDateStr(DATE_PATTERN) + ".log")
            // 设置输出文件的等级（如果FileHandler的等级高于或者等于log的level，则按照FileHandler的level输出到文件，如果低于，则按照Log等级输出）
            fileHandler.level = level
            fileHandler.formatter = object : Formatter() {
                override fun format(record: LogRecord): String {
                    // 设置文件输出格式
                    return (getCurrentDateStr(TIME_PATTERN) + ":" + record.message + "\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // 添加输出文件handler
        log.addHandler(fileHandler)
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