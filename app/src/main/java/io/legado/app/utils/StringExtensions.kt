package io.legado.app.utils

fun String?.strim() = if (this.isNullOrBlank()) null else this.trim()