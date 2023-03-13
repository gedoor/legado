package io.legado.app.utils.compress

import androidx.annotation.Keep
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Keep
@Suppress("unused", "MemberVisibilityCanBePrivate")
object RarUtils {

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(inputStream: InputStream, path: String) {
        unRarToPath(inputStream, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, path: String) {
        unRarToPath(byteArray, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(zipPath: String, path: String) {
        unRarToPath(zipPath, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, path: String) {
        unRarToPath(file, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(inputStream: InputStream, destDir: File?) {
        Archive(inputStream).use {
            unRarToPath(it, destDir)
        }
        inputStream.close()
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(byteArray: ByteArray, destDir: File?) {
        Archive(ByteArrayInputStream(byteArray)).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(filePath: String, destDir: File?) {
        Archive(File(filePath)).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(file: File, destDir: File?) {
        Archive(file).use {
            unRarToPath(it, destDir)
        }
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun unRarToPath(archive: Archive, destDir: File?) {
        destDir ?: throw NullPointerException("解决路径不能为空")
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
            }
        }
    }

}