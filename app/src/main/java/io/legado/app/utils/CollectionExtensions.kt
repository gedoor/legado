package io.legado.app.utils

fun List<Float>.fastSum(): Float {
    var sum = 0f
    for (i in indices) {
        sum += this[i]
    }
    return sum
}

inline fun <T> List<T>.fastBinarySearch(
    fromIndex: Int = 0,
    toIndex: Int = size,
    comparison: (T) -> Int
): Int {
    var low = fromIndex
    var high = toIndex - 1

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = get(mid)
        val cmp = comparison(midVal)

        if (cmp < 0)
            low = mid + 1
        else if (cmp > 0)
            high = mid - 1
        else
            return mid // key found
    }
    return -(low + 1)  // key not found
}

inline fun <T, K : Comparable<K>> List<T>.fastBinarySearchBy(
    key: K?,
    fromIndex: Int = 0,
    toIndex: Int = size,
    crossinline selector: (T) -> K?
): Int = fastBinarySearch(fromIndex, toIndex) { compareValues(selector(it), key) }
