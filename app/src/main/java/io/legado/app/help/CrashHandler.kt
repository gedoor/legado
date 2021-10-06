package io.legado.app.help

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import io.legado.app.constant.AppConst
import io.legado.app.model.ReadAloud
import io.legado.app.utils.FileUtils
import io.legado.app.utils.longToastOnUi
import io.legado.app.utils.msg
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 异常管理类
 */
@Suppress("DEPRECATION")
class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {
    private val tag = this.javaClass.simpleName

    /**
     * 系统默认UncaughtExceptionHandler
     */
    private var mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    /**
     * 存储异常和参数信息
     */
    private val paramsMap = HashMap<String, String>()

    /**
     * 格式化时间
     */
    @SuppressLint("SimpleDateFormat")
    private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

    init {
        //设置该CrashHandler为系统默认的
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * uncaughtException 回调函数
     */
    override fun uncaughtException(thread: Thread, ex: Throwable) {
        ReadAloud.stop(context)
        handleException(ex)
        mDefaultHandler?.uncaughtException(thread, ex)
    }

    /**
     * 处理该异常
     */
    private fun handleException(ex: Throwable?) {
        if (ex == null) return
        //收集设备参数信息
        collectDeviceInfo(context)
        //添加自定义信息
        addCustomInfo()
        //保存日志文件
        saveCrashInfo2File(ex)
        context.longToastOnUi(ex.msg)
    }

    /**
     * 收集设备参数信息
     */
    private fun collectDeviceInfo(ctx: Context) {
        kotlin.runCatching {
            //获取系统信息
            paramsMap["MANUFACTURER"] = Build.MANUFACTURER
            paramsMap["BRAND"] = Build.BRAND
            //获取app版本信息
            AppConst.appInfo.let {
                paramsMap["versionName"] = it.versionName
                paramsMap["versionCode"] = it.versionCode.toString()
            }
        }
    }

    /**
     * 添加自定义参数
     */
    private fun addCustomInfo() {
        Log.i(tag, "addCustomInfo: 程序出错了...")
    }

    /**
     * 保存错误信息到文件中
     */
    private fun saveCrashInfo2File(ex: Throwable) {
        val sb = StringBuilder()
        for ((key, value) in paramsMap) {
            sb.append(key).append("=").append(value).append("\n")
        }

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause: Throwable? = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()
        val result = writer.toString()
        sb.append(result)
        val timestamp = System.currentTimeMillis()
        val time = format.format(Date())
        val fileName = "crash-$time-$timestamp.log"
        context.externalCacheDir?.let { rootFile ->
            FileUtils.getFile(rootFile, "crash").listFiles()?.forEach {
                if (it.lastModified() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)) {
                    it.delete()
                }
            }
            FileUtils.createFileIfNotExist(rootFile, "crash", fileName)
                .writeText(sb.toString())
        }
    }

}
