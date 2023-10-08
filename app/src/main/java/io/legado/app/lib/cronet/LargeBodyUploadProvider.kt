package io.legado.app.lib.cronet

import androidx.annotation.Keep
import okhttp3.RequestBody
import okio.BufferedSource
import okio.Pipe
import okio.buffer
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService

/**
 * 用于上传大型文件
 *
 * @property body
 * @property executorService
 */
@Keep
class LargeBodyUploadProvider(
    private val body: RequestBody,
    private val executorService: ExecutorService
) : UploadDataProvider(), AutoCloseable {
    private val pipe = Pipe(BUFFER_SIZE.toLong())
    private var source: BufferedSource = pipe.source.buffer()

    @Volatile
    private var filled: Boolean = false
    override fun getLength(): Long {
        return body.contentLength()
    }

    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        if (!filled) {
            fillBuffer()
        }
        check(byteBuffer.hasRemaining()) { "Cronet passed a buffer with no bytes remaining" }
        var read: Int
        var bytesRead = 0
        while (bytesRead <= 0) {
            read = source.read(byteBuffer)
            bytesRead += read
        }
        uploadDataSink.onReadSucceeded(false)
    }

    @Synchronized
    private fun fillBuffer() {
        executorService.submit {
            try {
                val writeSink = pipe.sink.buffer()
                filled = true
                body.writeTo(writeSink)
                writeSink.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

    }

    override fun rewind(p0: UploadDataSink?) {
        check(body.isOneShot()) { "Okhttp RequestBody is OneShot" }
        filled = false
        fillBuffer()
    }

    override fun close() {
//        pipe.cancel()
//        source.close()
        super.close()
    }
}