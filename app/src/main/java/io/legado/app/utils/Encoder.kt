package io.legado.app.utils

object Encoder {

    fun escape(src: String): String {
        var i = 0
        var char: Char
        val tmp = StringBuilder()
        tmp.ensureCapacity(src.length * 6)
        while (i < src.length) {
            char = src[i]
            if (Character.isDigit(char) || Character.isLowerCase(char)
                || Character.isUpperCase(char)
            )
                tmp.append(char)
            else if (char.toInt() < 256) {
                tmp.append("%")
                if (char.toInt() < 16)
                    tmp.append("0")
                tmp.append(char.toInt().toString(16))
            } else {
                tmp.append("%u")
                tmp.append(char.toInt().toString(16))
            }
            i++
        }
        return tmp.toString()
    }
}