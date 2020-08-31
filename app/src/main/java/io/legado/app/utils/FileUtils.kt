package io.legado.app.utils

import android.os.Environment
import io.legado.app.App
import java.io.File
import java.io.IOException

@Suppress("unused")
object FileUtils {

    fun exists(root: File, fileName: String, vararg subDirs: String): Boolean {
        return getFile(root, fileName, subDirs = subDirs).exists()
    }

    fun createFileIfNotExist(root: File, fileName: String, vararg subDirs: String): File {
        val filePath = getPath(fileName, root, *subDirs)
        return createFileIfNotExist(filePath)
    }

    fun createFolderIfNotExist(root: File, vararg subDirs: String): File {
        val filePath = getPath(root = root, subDirs = subDirs)
        return createFolderIfNotExist(filePath)
    }

    fun createFolderIfNotExist(filePath: String): File {
        val file = File(filePath)
        //如果文件夹不存在，就创建它
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    @Synchronized
    fun createFileIfNotExist(filePath: String): File {
        val file = File(filePath)
        try {
            if (!file.exists()) {
                //创建父类文件夹
                file.parent?.let {
                    createFolderIfNotExist(it)
                }
                //创建文件
                file.createNewFile()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    fun getFile(root: File, fileName: String, vararg subDirs: String): File {
        val filePath = getPath(fileName, root, *subDirs)
        return File(filePath)
    }

    fun getDirFile(root: File, vararg subDirs: String): File {
        val filePath = getPath(root = root, subDirs = subDirs)
        return File(filePath)
    }

    fun getPath(fileName: String? = null, root: File, vararg subDirs: String): String {
        val path = StringBuilder(root.absolutePath)
        subDirs.forEach {
            path.append(File.separator).append(it)
        }
        if (!fileName.isNullOrEmpty()) {
            path.append(File.separator).append(fileName)
        }
        return path.toString()
    }

    //递归删除文件夹下的数据
    @Synchronized
    fun deleteFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return

        if (file.isDirectory) {
            val files = file.listFiles()
            files?.forEach { subFile ->
                val path = subFile.path
                deleteFile(path)
            }
        }
        //删除文件
        file.delete()
    }

    fun getCachePath(): String {
        return App.INSTANCE.externalCacheDir?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath
    }

    fun getSdCardPath(): String {
        @Suppress("DEPRECATION")
        var sdCardDirectory = Environment.getExternalStorageDirectory().absolutePath
        try {
            sdCardDirectory = File(sdCardDirectory).canonicalPath
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
        return sdCardDirectory
    }

}
