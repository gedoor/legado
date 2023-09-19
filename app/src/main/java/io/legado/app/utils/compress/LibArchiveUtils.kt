package io.legado.app.utils.compress

import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.OsConstants.S_IFDIR
import android.system.OsConstants.S_ISDIR
import io.legado.app.lib.icu4j.CharsetDetector
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import me.zhanghai.android.libarchive.ArchiveException
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.io.InterruptedIOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


object LibArchiveUtils {


//    @Throws(ArchiveException::class)
//    fun openArchive(
//        inputStream: InputStream,
//    ): Long {
//        val archive: Long = Archive.readNew()
//        var successful = false
//        try {
//            Archive.setCharset(archive, StandardCharsets.UTF_8.name().toByteArray())
//            Archive.readSupportFilterAll(archive)
//            Archive.readSupportFormatAll(archive)
//            Archive.readSetCallbackData(archive, null)
//            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
//            Archive.readSetReadCallback<Any?>(archive) { _, _ ->
//                buffer.clear()
//                val bytesRead = try {
//                    inputStream.read(buffer.array())
//                } catch (e: IOException) {
//                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.read", e)
//                }
//                if (bytesRead != -1) {
//                    buffer.limit(bytesRead)
//                    buffer
//                } else {
//                    null
//                }
//            }
//            Archive.readSetSkipCallback<Any?>(archive) { _, _, request ->
//                try {
//                    inputStream.skip(request)
//                } catch (e: IOException) {
//                    throw ArchiveException(Archive.ERRNO_FATAL, "InputStream.skip", e)
//                }
//            }
//            Archive.readOpen1(archive)
//            successful = true
//            return archive
//
//
//        } finally {
//            if (!successful) {
//                Archive.free(archive)
//            }
//        }
//
//
//    }


    @Throws(ArchiveException::class)
    private fun openArchive(
        channel: SeekableByteChannel,
    ): Long {
        val archive: Long = Archive.readNew()
        var successful = false
        try {
            Archive.setCharset(archive, StandardCharsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            Archive.readSetCallbackData(archive, null)
            val buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
            Archive.readSetReadCallback<Any?>(archive) { _, _ ->
                buffer.clear()
                val bytesRead = try {
                    channel.read(buffer)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.read", e)
                }
                if (bytesRead != -1) {
                    buffer.flip()
                    buffer
                } else {
                    null
                }
            }
            Archive.readSetSkipCallback<Any?>(archive) { _, _, request ->
                try {
                    channel.position(channel.position() + request)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.position", e)
                }
                request
            }
            Archive.readSetSeekCallback<Any?>(archive) { _, _, offset, whence ->
                val newPosition: Long
                try {
                    newPosition = when (whence) {
                        OsConstants.SEEK_SET -> offset
                        OsConstants.SEEK_CUR -> channel.position() + offset
                        OsConstants.SEEK_END -> channel.size() + offset
                        else -> throw ArchiveException(
                            Archive.ERRNO_FATAL,
                            "Unknown whence $whence"
                        )
                    }
                    channel.position(newPosition)
                } catch (e: IOException) {
                    throw ArchiveException(Archive.ERRNO_FATAL, "SeekableByteChannel.position", e)
                }
                newPosition
            }
            Archive.readOpen1(archive)
            successful = true
            return archive


        } finally {
            if (!successful) {
                Archive.free(archive)
            }
        }


    }


    @Throws(ArchiveException::class)
    private fun openArchive(
        pfd: ParcelFileDescriptor,
        useCb: Boolean = true
    ): Long {
        val archive: Long = Archive.readNew()
        var successful = false
        try {
            Archive.setCharset(archive, StandardCharsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            if (useCb) {
                Archive.readSetCallbackData(archive, pfd.fileDescriptor)
                val buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                Archive.readSetReadCallback<Any>(
                    archive
                ) { _1: Long, fd: Any? ->
                    buffer.clear()
                    try {
                        Os.read(fd as FileDescriptor?, buffer)
                    } catch (e: ErrnoException) {
                        throw ArchiveException(Archive.ERRNO_FATAL, "Os.read", e)
                    } catch (e: InterruptedIOException) {
                        throw ArchiveException(Archive.ERRNO_FATAL, "Os.read", e)
                    }
                    buffer.flip()
                    buffer
                }
                Archive.readSetSkipCallback<Any>(
                    archive
                ) { _1: Long, fd: Any?, request: Long ->
                    try {
                        Os.lseek(
                            fd as FileDescriptor?, request, OsConstants.SEEK_CUR
                        )
                    } catch (e: ErrnoException) {
                        throw ArchiveException(Archive.ERRNO_FATAL, "Os.lseek", e)
                    }
                    request
                }
                Archive.readSetSeekCallback<Any>(
                    archive
                ) { _1: Long, fd: Any?, offset: Long, whence: Int ->
                    try {
                        return@readSetSeekCallback Os.lseek(
                            fd as FileDescriptor?, offset, whence
                        )
                    } catch (e: ErrnoException) {
                        throw ArchiveException(Archive.ERRNO_FATAL, "Os.lseek", e)
                    }
                }
                Archive.readOpen1(archive)

            } else {
                Archive.readOpenFd(archive, pfd.fd, DEFAULT_BUFFER_SIZE.toLong())
            }

            successful = true
            return archive


        } finally {
            if (!successful) {
                Archive.free(archive)
            }
        }


    }


    @Throws(NullPointerException::class, SecurityException::class)
    fun unArchive(
        pfd: ParcelFileDescriptor,
        destDir: File,
        filter: ((String) -> Boolean)?
    ): List<File> {
        return unArchive(openArchive(pfd), destDir, filter)
    }


    @Throws(NullPointerException::class, SecurityException::class)
    private fun unArchive(
        archive: Long,
        destDir: File?,
        filter: ((String) -> Boolean)? = null
    ): List<File> {
        destDir ?: throw NullPointerException("解决路径不能为空")
        val files = arrayListOf<File>()


        try {
            var entry = Archive.readNextHeader(archive)
            while (entry != 0L) {
                val entryName =
                    getEntryString(ArchiveEntry.pathnameUtf8(entry), ArchiveEntry.pathname(entry))
                        ?: continue
                val entryFile = File(destDir, entryName)
                if (!entryFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    throw SecurityException("压缩文件只能解压到指定路径")
                }
                val entryStat = ArchiveEntry.stat(entry)

                //判断是否是文件夹
                if (S_ISDIR(entryStat.stMode)) {
                    if (!entryFile.exists()) {
                        entryFile.mkdirs()
                    }
                    entry = Archive.readNextHeader(archive)
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

                ParcelFileDescriptor.open(entryFile, ParcelFileDescriptor.MODE_WRITE_ONLY).use {
                    Archive.readDataIntoFd(archive, it.fd)
                    files.add(entryFile)
                }

                entry = Archive.readNextHeader(archive)


            }
        } finally {
            Archive.free(archive)
        }





        return files


    }

    fun getFilesName(pfd: ParcelFileDescriptor, filter: ((String) -> Boolean)?): List<String> {
        return getFilesName(openArchive(pfd), filter)
    }


    @Throws(SecurityException::class)
    private fun getFilesName(
        archive: Long,
        filter: ((String) -> Boolean)? = null
    ): List<String> {
        val fileNames = mutableListOf<String>()
        try {
            //Archive.readOpenFd(archive, pfd.fd, 8192)

            var entry = Archive.readNextHeader(archive)
            //val formatName: String = newStringFromBytes(Archive.formatName(archive))
            while (entry != 0L) {
                val fileName =
                    getEntryString(ArchiveEntry.pathnameUtf8(entry), ArchiveEntry.pathname(entry))
                        ?: continue


                val entryStat = ArchiveEntry.stat(entry)
                val fileType = entryStat.stMode and S_IFDIR


                if (S_ISDIR(entryStat.stMode)) {
                    entry = Archive.readNextHeader(archive)
                    continue
                }

                if (filter != null && filter.invoke(fileName))
                    fileNames.add(fileName)

                entry = Archive.readNextHeader(archive)


            }
        } finally {
            Archive.free(archive)
        }


        return fileNames
    }


    private fun getEntryString(utf8: String?, bytes: ByteArray?): String? {
        return utf8 ?: newStringFromBytes(bytes)
    }

    private fun newStringFromBytes(bytes: ByteArray?): String? {
        bytes ?: return null
        val cd = CharsetDetector()
        cd.setText(bytes)
        val c = cd.detectAll().first().name
        return String(bytes, Charset.forName(c))

    }


}