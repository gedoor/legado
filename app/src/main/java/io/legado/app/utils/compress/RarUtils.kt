package io.legado.app.utils.compress

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Suppress("unused", "MemberVisibilityCanBePrivate")
object RarUtils {

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(inputStream: InputStream, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return unRarToPath(inputStream, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return unRarToPath(byteArray, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(zipPath: String, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return unRarToPath(zipPath, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return unRarToPath(file, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(inputStream: InputStream, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return Archive(inputStream).use {
            unRarToPath(it, destDir, filter)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return Archive(ByteArrayInputStream(byteArray)).use {
            unRarToPath(it, destDir, filter)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(filePath: String, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return Archive(File(filePath)).use {
            unRarToPath(it, destDir, filter)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return Archive(file).use {
            unRarToPath(it, destDir, filter)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun unRarToPath(
        archive: Archive, 
        destDir: File?,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        destDir ?: throw NullPointerException("解决路径不能为空")
        val files = arrayListOf<File>()
        var entry: FileHeader?
        while (archive.nextFileHeader().also { entry = it } != null) {
            val entryName = entry!!.fileName
            val entryFile = File(destDir, entryName)
            if (!entryFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                throw SecurityException("压缩文件只能解压到指定路径")
            }
            if (entry!!.isDirectory) {
                if (!entryFile.exists()) {
                    entryFile.mkdirs()
                }
                continue
            }
            if (entryFile.parentFile?.exists() != true) {
                entryFile.parentFile?.mkdirs()
            }
            if (filter != null && !filter.invoke(entryName)) continue
            if (!entryFile.exists()) {
                entryFile.createNewFile()
                entryFile.setReadable(true)
                entryFile.setExecutable(true)
            }
            FileOutputStream(entryFile).use {
                archive.getInputStream(entry).copyTo(it)
                files.add(entryFile)
            }
        }
        return files
    }


    /* 遍历目录获取所有文件名 */
    @Throws(NullPointerException::class, SecurityException::class)
    fun getFilesName(
        inputStream: InputStream,
        filter: ((String) -> Boolean)? = null
    ): List<String> {
        return Archive(inputStream).use {
            getFilesName(it, filter)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun getFilesName(
        archive: Archive,
        filter: ((String) -> Boolean)? = null
    ): List<String> {
        val fileNames = mutableListOf<String>()
        var entry: FileHeader?
        while (archive.nextFileHeader().also { entry = it } != null) {
            if (entry!!.isDirectory) {
                continue
            }
            val fileName = entry!!.fileName
            if (filter != null && filter.invoke(fileName))
                fileNames.add(fileName)
        }
        return fileNames
    }

}