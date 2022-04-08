package io.legado.app.utils

@Suppress("unused")
object Utf8BomUtils {
    private val UTF8_BOM_BYTES = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

    fun removeUTF8BOM(xmlText: String): String {
        val bytes = xmlText.toByteArray()
        val containsBOM = (bytes.size > 3
                && bytes[0] == UTF8_BOM_BYTES[0]
                && bytes[1] == UTF8_BOM_BYTES[1]
                && bytes[2] == UTF8_BOM_BYTES[2])
        if (containsBOM) {
            return String(bytes, 3, bytes.size - 3)
        }
        return xmlText
    }

    fun removeUTF8BOM(bytes: ByteArray): ByteArray {
        val containsBOM = (bytes.size > 3
                && bytes[0] == UTF8_BOM_BYTES[0]
                && bytes[1] == UTF8_BOM_BYTES[1]
                && bytes[2] == UTF8_BOM_BYTES[2])
        if (containsBOM) {
            val copy = ByteArray(bytes.size - 3)
            System.arraycopy(bytes, 3, copy, 0, bytes.size - 3)
            return copy
        }
        return bytes
    }

    fun hasBom(bytes: ByteArray): Boolean {
        return (bytes.size > 3
                && bytes[0] == UTF8_BOM_BYTES[0]
                && bytes[1] == UTF8_BOM_BYTES[1]
                && bytes[2] == UTF8_BOM_BYTES[2])
    }
}