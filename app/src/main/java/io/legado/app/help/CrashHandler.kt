package io.legado.app.help

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import io.legado.app.constant.AppConst
import io.legado.app.model.ReadAloud
import io.legado.app.utils.FileUtils
import io.legado.app.utils.getFile
import io.legado.app.utils.longToastOnUi
import io.legado.app.utils.stackTraceStr
import splitties.init.appCtx
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

    /**
     * 系统默认UncaughtExceptionHandler
     */
    private var mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()

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
        //保存日志文件
        saveCrashInfo2File(ex)
        context.longToastOnUi(ex.stackTraceStr)
        Thread.sleep(3000)
    }

    companion object {
        /**
         * 存储异常和参数信息
         */
        private val paramsMap by lazy {
            val map = HashMap<String, String>()
            kotlin.runCatching {
                //获取系统信息
                map["MANUFACTURER"] = Build.MANUFACTURER
                map["BRAND"] = Build.BRAND
                map["MODEL"] = Build.MODEL
                map["SDK_INT"] = Build.VERSION.SDK_INT.toString()
                map["RELEASE"] = Build.VERSION.RELEASE
                //获取app版本信息
                AppConst.appInfo.let {
                    map["versionName"] = it.versionName
                    map["versionCode"] = it.versionCode.toString()
                }
            }
            map
        }

        /**
         * 格式化时间
         */
        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

        /**
         * 保存错误信息到文件中
         */
        fun saveCrashInfo2File(ex: Throwable) {
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
            appCtx.externalCacheDir?.let { rootFile ->
                rootFile.getFile("crash").listFiles()?.forEach {
                    if (it.lastModified() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)) {
                        it.delete()
                    }
                }
                FileUtils.createFileIfNotExist(rootFile, "crash", fileName)
                    .writeText(sb.toString())
            }
        }

    }

}
