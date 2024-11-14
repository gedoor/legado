package io.legado.app.lib.mobi.entities

data class KF6Section(
    val index: Int,
    val start: Int,
    val end: Int,
    val length: Int,
    val href: String,
    var next: KF6Section? = null
)
