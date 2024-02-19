package io.legado.app.lib.cronet

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.text.TextUtils
import androidx.annotation.Keep
import io.legado.app.BuildConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.Cronet
import io.legado.app.utils.DebugLog
import io.legado.app.utils.printOnDebug
import org.chromium.net.CronetEngine
import org.json.JSONObject
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Objects

@Suppress("ConstPropertyName")
@Keep
object CronetLoader : CronetEngine.Builder.LibraryLoader(), Cronet.LoaderInterface {
    //https://storage.googleapis.com/chromium-cronet/android/92.0.4515.159/Release/cronet/libs/arm64-v8a/libcronet.92.0.4515.159.so

    private const val soVersion = BuildConfig.Cronet_Version
    private const val soName = "libcronet.$soVersion.so"
    private val soUrl: String
    private val soFile: File
    private val downloadFile: File
    private var cpuAbi: String? = null
    private var md5: String
    var download = false

    @Volatile
    private var cacheInstall = false

    init {
        soUrl = ("https://storage.googleapis.com/chromium-cronet/android/"
                + soVersion + "/Release/cronet/libs/"
                + getCpuAbi(appCtx) + "/" + soName)
        md5 = getMd5(appCtx)
        val dir = appCtx.getDir("cronet", Context.MODE_PRIVATE)
        soFile = File(dir.toString() + "/" + getCpuAbi(appCtx), soName)
        downloadFile = File(appCtx.cacheDir.toString() + "/so_download", soName)
        DebugLog.d(javaClass.simpleName, "soName+:$soName")
        DebugLog.d(javaClass.simpleName, "destSuccessFile:$soFile")
        DebugLog.d(javaClass.simpleName, "tempFile:$downloadFile")
        DebugLog.d(javaClass.simpleName, "soUrl:$soUrl")
    }

    /**
     * 判断Cronet是否安装完成
     */
    override fun install(): Boolean {
        synchronized(this) {
            if (cacheInstall) {
                return true
            }
        }

        if (md5.length != 32 || !soFile.exists() || md5 != getFileMD5(soFile)) {
            cacheInstall = false
            return cacheInstall
        }
        cacheInstall = soFile.exists()
        return cacheInstall
    }


    /**
     * 预加载Cronet
     */
    override fun preDownload() {
        Coroutine.async {
            //md5 = getUrlMd5(md5Url)
            if (soFile.exists() && md5 == getFileMD5(soFile)) {
                DebugLog.d(javaClass.simpleName, "So 库已存在")
            } else {
                download(soUrl, md5, downloadFile, soFile)
            }
            DebugLog.d(javaClass.simpleName, soName)
        }
    }

    private fun getMd5(context: Context): String {
        val stringBuilder = StringBuilder()
        return try {
            //获取assets资源管理器
            val assetManager = context.assets
            //通过管理器打开文件并读取
            val bf = BufferedReader(
                InputStreamReader(
                    assetManager.open("cronet.json")
                )
            )
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            JSONObject(stringBuilder.toString()).optString(getCpuAbi(context), "")
        } catch (e: java.lang.Exception) {
            return ""
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    override fun loadLibrary(libName: String) {
        DebugLog.d(javaClass.simpleName, "libName:$libName")
        val start = System.currentTimeMillis()
        @Suppress("SameParameterValue")
        try {
            //非cronet的so调用系统方法加载
            if (!libName.contains("cronet")) {
                System.loadLibrary(libName)
                return
            }
            //以下逻辑为cronet加载，优先加载本地，否则从远程加载
            //首先调用系统行为进行加载
            System.loadLibrary(libName)
            DebugLog.d(javaClass.simpleName, "load from system")
        } catch (e: Throwable) {
            //如果找不到，则从远程下载
            //删除历史文件
            deleteHistoryFile(Objects.requireNonNull(soFile.parentFile), soFile)
            //md5 = getUrlMd5(md5Url)
            DebugLog.d(javaClass.simpleName, "soMD5:$md5")
            if (md5.length != 32 || soUrl.isEmpty()) {
                //如果md5或下载的url为空，则调用系统行为进行加载
                System.loadLibrary(libName)
                return
            }
            if (!soFile.exists() || !soFile.isFile) {
                soFile.delete()
                download(soUrl, md5, downloadFile, soFile)
                //如果文件不存在或不是文件，则调用系统行为进行加载
                System.loadLibrary(libName)
                return
            }
            if (soFile.exists()) {
                //如果文件存在，则校验md5值
                val fileMD5 = getFileMD5(soFile)
                if (fileMD5 != null && fileMD5.equals(md5, ignoreCase = true)) {
                    //md5值一样，则加载
                    System.load(soFile.absolutePath)
                    DebugLog.d(javaClass.simpleName, "load from:$soFile")
                    return
                }
                //md5不一样则删除
                soFile.delete()
            }
            //不存在则下载
            download(soUrl, md5, downloadFile, soFile)
            //使用系统加载方法
            System.loadLibrary(libName)
        } finally {
            DebugLog.d(javaClass.simpleName, "time:" + (System.currentTimeMillis() - start))
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getCpuAbi(context: Context): String? {
        if (cpuAbi != null) {
            return cpuAbi
        }
        // 5.0以上Application才有primaryCpuAbi字段
        try {
            val appInfo = context.applicationInfo
            val abiField = ApplicationInfo::class.java.getDeclaredField("primaryCpuAbi")
            abiField.isAccessible = true
            cpuAbi = abiField.get(appInfo) as String?
        } catch (e: Exception) {
            e.printOnDebug()
        }
        if (TextUtils.isEmpty(cpuAbi)) {
            cpuAbi = Build.SUPPORTED_ABIS[0]
        }
        return cpuAbi
    }


    /**
     * 删除历史文件
     */
    private fun deleteHistoryFile(dir: File, currentFile: File?) {
        val files = dir.listFiles()
        @Suppress("SameParameterValue")
        if (files != null && files.isNotEmpty()) {
            for (f in files) {
                if (f.exists() && (currentFile == null || f.absolutePath != currentFile.absolutePath)) {
                    val delete = f.delete()
                    DebugLog.d(javaClass.simpleName, "delete file: $f result: $delete")
                    if (!delete) {
                        f.deleteOnExit()
                    }
                }
            }
        }
    }

    /**
     * 下载文件
     */
    private fun downloadFileIfNotExist(url: String, destFile: File): Boolean {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            inputStream = connection.inputStream
            if (destFile.exists()) {
                return true
            }
            destFile.parentFile!!.mkdirs()
            destFile.createNewFile()
            outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(32768)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                outputStream.flush()
            }
            return true
        } catch (e: Throwable) {
            e.printOnDebug()
            if (destFile.exists() && !destFile.delete()) {
                destFile.deleteOnExit()
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printOnDebug()
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printOnDebug()
                }
            }
        }
        return false
    }

    /**
     * 下载并拷贝文件
     */
    @Suppress("SameParameterValue")
    @Synchronized
    private fun download(
        url: String,
        md5: String?,
        downloadTempFile: File,
        destSuccessFile: File
    ) {
        if (download) {
            return
        }
        download = true

        Coroutine.async {
            val result = downloadFileIfNotExist(url, downloadTempFile)
            DebugLog.d(javaClass.simpleName, "download result:$result")
            //文件md5再次校验
            val fileMD5 = getFileMD5(downloadTempFile)
            if (md5 != null && !md5.equals(fileMD5, ignoreCase = true)) {
                val delete = downloadTempFile.delete()
                if (!delete) {
                    downloadTempFile.deleteOnExit()
                }
                download = false
                return@async
            }
            DebugLog.d(javaClass.simpleName, "download success, copy to $destSuccessFile")
            //下载成功拷贝文件
            copyFile(downloadTempFile, destSuccessFile)
            cacheInstall = false
            val parentFile = downloadTempFile.parentFile
            @Suppress("SameParameterValue")
            (deleteHistoryFile(parentFile!!, null))
        }
    }

    /**
     * 拷贝文件
     */
    private fun copyFile(source: File?, dest: File?): Boolean {
        if (source == null || !source.exists() || !source.isFile || dest == null) {
            return false
        }
        if (source.absolutePath == dest.absolutePath) {
            return true
        }
        var fileInputStream: FileInputStream? = null
        var os: FileOutputStream? = null
        val parent = dest.parentFile
        if (parent != null && !parent.exists()) {
            val mkdirs = parent.mkdirs()
            if (!mkdirs) {
                parent.mkdirs()
            }
        }
        try {
            fileInputStream = FileInputStream(source)
            os = FileOutputStream(dest, false)
            val buffer = ByteArray(1024 * 512)
            var length: Int
            while (fileInputStream.read(buffer).also { length = it } > 0) {
                os.write(buffer, 0, length)
            }
            return true
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
        }
        return false
    }

    /**
     * 获得文件md5
     */
    private fun getFileMD5(file: File): String? {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(file)
            val md5 = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(1024)
            var numRead: Int
            while (fileInputStream.read(buffer).also { numRead = it } > 0) {
                md5.update(buffer, 0, numRead)
            }
            return String.format("%032x", BigInteger(1, md5.digest())).lowercase()
        } catch (e: Exception) {
            e.printOnDebug()
        } catch (e: OutOfMemoryError) {
            e.printOnDebug()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
        }
        return null
    }

}