package io.legado.app.utils

import android.annotation.SuppressLint
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

@Suppress("unused")
@SuppressLint("NewApi")
class ParcelFileDescriptorChannel(private val pfd: ParcelFileDescriptor) : SeekableByteChannel {
    @Throws(IOException::class)
    override fun read(dst: ByteBuffer): Int {
        return try {
            Os.read(pfd.fileDescriptor, dst)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun write(src: ByteBuffer): Int {
        return try {
            Os.write(pfd.fileDescriptor, src)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun position(): Long {
        return try {
            Os.lseek(pfd.fileDescriptor, 0, OsConstants.SEEK_CUR)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun position(newPosition: Long): SeekableByteChannel {
        try {
            Os.lseek(pfd.fileDescriptor, newPosition, OsConstants.SEEK_SET)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
        return this
    }

    @Throws(IOException::class)
    override fun size(): Long {
        return try {
            Os.fstat(pfd.fileDescriptor).st_size
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun truncate(newSize: Long): SeekableByteChannel {
        require(!(newSize < 0L || newSize > Int.MAX_VALUE)) { "Size has to be in range 0.. " + Int.MAX_VALUE }
        try {
            Os.ftruncate(pfd.fileDescriptor, newSize)
        } catch (e: ErrnoException) {
            throw RuntimeException(e)
        }
        return this
    }

    override fun isOpen(): Boolean {
        return pfd.fileDescriptor.valid()
    }

    @Throws(IOException::class)
    override fun close() {
        pfd.close()
    }
}