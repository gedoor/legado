package io.legado.app.help.http.cronet

import okhttp3.RequestBody
import okio.Buffer
import org.chromium.net.UploadDataProvider
import org.chromium.net.UploadDataSink
import java.io.IOException
import java.nio.ByteBuffer

class BodyUploadProvider(body: RequestBody) : UploadDataProvider(), AutoCloseable {
    private val body: RequestBody
    private val buffer: Buffer?

    init {
        buffer = Buffer()
        this.body = body
        try {
            body.writeTo(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun getLength(): Long {
        return body.contentLength()
    }

    @Throws(IOException::class)
    override fun read(uploadDataSink: UploadDataSink, byteBuffer: ByteBuffer) {
        check(byteBuffer.hasRemaining()) { "Cronet passed a buffer with no bytes remaining" }
        var read: Int
        var bytesRead = 0
        while (bytesRead == 0) {
            read = buffer!!.read(byteBuffer)
            bytesRead += read
        }
        uploadDataSink.onReadSucceeded(false)
    }

    @Throws(IOException::class)
    override fun rewind(uploadDataSink: UploadDataSink) {
        uploadDataSink.onRewindSucceeded()
    }

    @Throws(IOException::class)
    override fun close() {
        buffer?.close()
        super.close()
    }
}