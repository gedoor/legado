package io.legado.app.utils

import okio.Buffer
import okio.EOFException
import okio.GzipSource
import okio.Source

class GzipSourceCompat(source: Source) : Source {
    private val delegate = GzipSource(source)

    override fun close() = delegate.close()

    override fun read(sink: Buffer, byteCount: Long): Long {
        try {
            return delegate.read(sink, byteCount)
        } catch (e: EOFException) {
            if (e.message == "source exhausted prematurely") {
                return -1
            }
            throw e
        }
    }

    override fun timeout() = delegate.timeout()

}
