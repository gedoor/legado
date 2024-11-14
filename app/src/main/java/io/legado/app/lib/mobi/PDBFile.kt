package io.legado.app.lib.mobi

import android.os.ParcelFileDescriptor
import io.legado.app.lib.mobi.utils.readString
import io.legado.app.lib.mobi.utils.readUInt16
import io.legado.app.lib.mobi.utils.readUInt32
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class PDBFile(private val pfd: ParcelFileDescriptor) {
    private val fc: FileChannel = FileInputStream(pfd.fileDescriptor).channel
    private val offsets: IntArray
    val name: String
    val type: String
    val creator: String
    val recordCount: Int

    init {
        var buffer = ByteBuffer.allocate(79)
        fc.read(buffer)
        name = buffer.readString(0, 32)
        type = buffer.readString(60, 4)
        creator = buffer.readString(64, 4)
        recordCount = buffer.readUInt16(76)

        buffer = ByteBuffer.allocate(recordCount * 8)
        fc.read(buffer, 78)
        offsets = IntArray(recordCount) {
            buffer.readUInt32(it * 8)
        }
    }

    fun getRecordData(index: Int): ByteBuffer {
        if (index < 0 || index >= recordCount) {
            throw IndexOutOfBoundsException("Record index out of bounds")
        }
        val len = offsets.getOrElse(index + 1) { fc.size().toInt() } - offsets[index]
        val buffer = ByteBuffer.allocate(len)
        fc.read(buffer, offsets[index].toLong())
        return buffer
    }

    fun close() {
        fc.close()
        pfd.close()
    }

}
