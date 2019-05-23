package io.legado.app.utils

fun String?.strim() = if (this.isNullOrBlank()) null else this.trim()

fun String.isAbsUrl() = this.startsWith("http://", true)
        || this.startsWith("https://", true)
