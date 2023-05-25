package io.legado.app.utils.compress

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import io.legado.app.utils.ParcelFileDescriptorChannel
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.channels.FileChannel

@Suppress("unused", "MemberVisibilityCanBePrivate")
object SevenZipUtils {

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(inputStream: InputStream, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(inputStream, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(byteArray: ByteArray, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(byteArray, File(path),filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(pfd: ParcelFileDescriptor, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(pfd, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(fileChannel: FileChannel, path: String, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(fileChannel, File(path), filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(inputStream: InputStream, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(SeekableInMemoryByteChannel(inputStream.readBytes())), destDir, filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(byteArray: ByteArray, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(SeekableInMemoryByteChannel(byteArray)), destDir, filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(pfd: ParcelFileDescriptor, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(ParcelFileDescriptorChannel(pfd)), destDir, filter)
    }

    @SuppressLint("NewApi")
    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(fileChannel: FileChannel, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(fileChannel), destDir, filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(file: File, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(file), destDir, filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    fun un7zToPath(filePath: String, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        return un7zToPath(SevenZFile(File(filePath)), destDir, filter)
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun un7zToPath(sevenZFile: SevenZFile, destDir: File?, filter: ((String) -> Boolean)? = null): List<File> {
        destDir ?: throw NullPointerException("解决路径不能为空")
        val files = arrayListOf<File>()
        var entry: SevenZArchiveEntry?
        while (sevenZFile.nextEntry.also { entry = it } != null) {
            val entryName = entry!!.name
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
                sevenZFile.getInputStream(entry).copyTo(it)
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
        return getFilesName(
           SevenZFile(SeekableInMemoryByteChannel(inputStream.readBytes())),
           filter
        )
    }

    @Throws(NullPointerException::class, SecurityException::class)
    private fun getFilesName(
        sevenZFile: SevenZFile,
        filter: ((String) -> Boolean)? = null
    ): List<String> {
        val fileNames = mutableListOf<String>()
        var entry: SevenZArchiveEntry?
        while (sevenZFile.nextEntry.also { entry = it } != null) {
            if (entry!!.isDirectory) {
                continue
            }
            val fileName = entry!!.name
            if (filter != null && filter.invoke(fileName))
                fileNames.add(fileName)
        }
        return fileNames
    }


}