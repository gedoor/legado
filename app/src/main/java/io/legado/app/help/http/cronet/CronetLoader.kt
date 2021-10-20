package io.legado.app.help.http.cronet

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.text.TextUtils
import com.google.android.gms.net.CronetProviderInstaller
import io.legado.app.BuildConfig
import io.legado.app.help.AppConfig
import io.legado.app.help.coroutine.Coroutine

import org.chromium.net.CronetEngine
import org.json.JSONObject
import splitties.init.appCtx
import timber.log.Timber
import java.io.*
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*


object CronetLoader : CronetEngine.Builder.LibraryLoader() {
    //https://storage.googleapis.com/chromium-cronet/android/92.0.4515.159/Release/cronet/libs/arm64-v8a/libcronet.92.0.4515.159.so

    private const val soVersion = BuildConfig.Cronet_Version
    private const val soName = "libcronet.$soVersion.so"
    private val soUrl: String
    private val soFile: File
    private val downloadFile: File
    private var cpuAbi: String? = null
    private var md5: String
    var download = false
    private var cacheInstall = false

    init {
        soUrl = ("https://storage.googleapis.com/chromium-cronet/android/"
                + soVersion + "/Release/cronet/libs/"
                + getCpuAbi(appCtx) + "/" + soName)
        md5 = getMd5(appCtx)
        val dir = appCtx.getDir("cronet", Context.MODE_PRIVATE)
        soFile = File(dir.toString() + "/" + getCpuAbi(appCtx), soName)
        downloadFile = File(appCtx.cacheDir.toString() + "/so_download", soName)
        Timber.d("soName+:$soName")
        Timber.d("destSuccessFile:$soFile")
        Timber.d("tempFile:$downloadFile")
        Timber.d("soUrl:$soUrl")
    }


    /**
     * 判断Cronet是否安装完成
     * @return
     */

    fun install(): Boolean {
        if (cacheInstall) {
            return true
        }
        if (AppConfig.isGooglePlay) {
            //检查GMS的Cronet服务是否安装
            cacheInstall = CronetProviderInstaller.isInstalled()
            return cacheInstall
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
    fun preDownload() {
        if (AppConfig.isGooglePlay) {
            CronetProviderInstaller.installProvider(appCtx)
            return
        }
        Coroutine.async {
            //md5 = getUrlMd5(md5Url)
            if (soFile.exists() && md5 == getFileMD5(soFile)) {
                Timber.d("So 库已存在")
            } else {
                download(soUrl, md5, downloadFile, soFile)
            }
            Timber.d(soName)
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
        Timber.d("libName:$libName")
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
            Timber.d("load from system")
        } catch (e: Throwable) {
            //如果找不到，则从远程下载
            //删除历史文件
            deleteHistoryFile(Objects.requireNonNull(soFile.parentFile), soFile)
            //md5 = getUrlMd5(md5Url)
            Timber.d("soMD5:$md5")
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
                    Timber.d("load from:$soFile")
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
            Timber.d("time:" + (System.currentTimeMillis() - start))
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
            Timber.e(e)
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
                    Timber.d("delete file: $f result: $delete")
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
            Timber.e(e)
            if (destFile.exists() && !destFile.delete()) {
                destFile.deleteOnExit()
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    Timber.e(e)
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    Timber.e(e)
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
        executor.execute {
            val result = downloadFileIfNotExist(url, downloadTempFile)
            Timber.d("download result:$result")
            //文件md5再次校验
            val fileMD5 = getFileMD5(downloadTempFile)
            if (md5 != null && !md5.equals(fileMD5, ignoreCase = true)) {
                val delete = downloadTempFile.delete()
                if (!delete) {
                    downloadTempFile.deleteOnExit()
                }
                download = false
                return@execute
            }
            Timber.d("download success, copy to $destSuccessFile")
            //下载成功拷贝文件
            copyFile(downloadTempFile, destSuccessFile)
            cacheInstall = false
            val parentFile = downloadTempFile.parentFile
            @Suppress("SameParameterValue")
            deleteHistoryFile(parentFile!!, null)
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
            Timber.e(e)
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: Exception) {
                    Timber.e(e)
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
            Timber.e(e)
        } catch (e: OutOfMemoryError) {
            Timber.e(e)
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
        return null
    }

}