package io.legado.app.lib.mobi.entities

data class KF8Section(
    val index: Int,
    val skeleton: Skeleton,
    val frags: List<Fragment>,
    val fragEnd: Int,
    val length: Int,
    val totalLength: Int,
    val href: String,
    var next: KF8Section? = null
) {
    val linear get() = frags.isNotEmpty()
}
