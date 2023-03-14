package io.legado.app.utils.compress

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import androidx.annotation.Keep
import io.legado.app.utils.ParcelFileDescriptorChannel
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.channels.FileChannel

@Keep
@Suppress("unused", "MemberVisibilityCanBePrivate")
object SevenZipUtils {

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(inputStream: InputStream, path: String): List<File> {
        return un7zToPath(inputStream, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(byteArray: ByteArray, path: String): List<File> {
        return un7zToPath(byteArray, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(pfd: ParcelFileDescriptor, path: String): List<File> {
        return un7zToPath(pfd, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(fileChannel: FileChannel, path: String): List<File> {
        return un7zToPath(fileChannel, File(path))
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(inputStream: InputStream, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(SeekableInMemoryByteChannel(inputStream.readBytes())), destDir)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(byteArray: ByteArray, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(SeekableInMemoryByteChannel(byteArray)), destDir)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(pfd: ParcelFileDescriptor, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(ParcelFileDescriptorChannel(pfd)), destDir)
    }

    @SuppressLint("NewApi")
    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(fileChannel: FileChannel, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(fileChannel), destDir)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(file: File, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(file), destDir)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(filePath: String, destDir: File?): List<File> {
        return un7zToPath(SevenZFile(File(filePath)), destDir)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun un7zToPath(sevenZFile: SevenZFile, destDir: File?): List<File> {
        destDir ?: throw NullPointerException("解决路径不能为空")
        val files = arrayListOf<File>()
        var entry: SevenZArchiveEntry?
        while (sevenZFile.nextEntry.also { entry = it } != null) {
            val entryFile = File(destDir, entry!!.name)
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
                sevenZFile.getInputStream(entry).copyTo(it)
                files.add(entryFile)
            }
        }
        return files
    }
}