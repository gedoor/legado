package io.legado.app.utils

fun List<Float>.fastSum(): Float {
    var sum = 0f
    for (i in indices) {
        sum += this[i]
    }
    return sum
}
