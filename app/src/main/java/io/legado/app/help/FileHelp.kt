package io.legado.app.help

import io.legado.app.App
import java.io.File
import java.io.IOException

object FileHelp {


    //获取文件夹
    fun getFolder(filePath: String): File {
        val file = File(filePath)
        //如果文件夹不存在，就创建它
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    //获取文件
    @Synchronized
    fun getFile(filePath: String): File {
        val file = File(filePath)
        try {
            if (!file.exists()) {
                //创建父类文件夹
                getFolder(file.parent)
                //创建文件
                file.createNewFile()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    fun getCachePath(): String {
        return App.INSTANCE.externalCacheDir?.absolutePath
            ?: App.INSTANCE.cacheDir.absolutePath
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
}