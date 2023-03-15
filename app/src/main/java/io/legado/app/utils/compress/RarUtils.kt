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
    fun unRarToPath(inputStream: InputStream, path: String): List<File> {
        return unRarToPath(inputStream, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, path: String): List<File> {
        return unRarToPath(byteArray, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(zipPath: String, path: String): List<File> {
        return unRarToPath(zipPath, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, path: String): List<File> {
        return unRarToPath(file, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(inputStream: InputStream, destDir: File?): List<File> {
        return Archive(inputStream).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, destDir: File?): List<File> {
        return Archive(ByteArrayInputStream(byteArray)).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(filePath: String, destDir: File?): List<File> {
        return Archive(File(filePath)).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, destDir: File?): List<File> {
        return Archive(file).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun unRarToPath(archive: Archive, destDir: File?): List<File> {
        destDir ?: throw NullPointerException("解决路径不能为空")
        val files = arrayListOf<File>()
        var entry: FileHeader?
        while (archive.nextFileHeader().also { entry = it } != null) {
            val entryFile = File(destDir, entry!!.fileName)
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

}