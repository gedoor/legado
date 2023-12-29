package io.legado.app.utils


/**
 * Search the data byte array for the first occurrence
 * of the byte array pattern.
 */
fun ByteArray.indexOf(pattern: ByteArray, start: Int = 0, stop: Int = size): Int {
    val data = this
    val failure: IntArray = computeFailure(pattern)

    var j = 0

    for (i in start until stop) {
        while (j > 0 && pattern[j] != data[i]) {
            j = failure[j - 1]
        }
        if (pattern[j] == data[i]) {
            j++
        }
        if (j == pattern.size) {
            return i - pattern.size + 1
        }
    }
    return -1
}

/**
 * Computes the failure function using a boot-strapping process,
 * where the pattern is matched against itself.
 */
private fun computeFailure(pattern: ByteArray): IntArray {
    val failure = IntArray(pattern.size)
    var j = 0
    for (i in 1 until pattern.size) {
        while (j > 0 && pattern[j] != pattern[i]) {
            j = failure[j - 1]
        }
        if (pattern[j] == pattern[i]) {
            j++
        }
        failure[i] = j
    }
    return failure
}
