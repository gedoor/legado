package io.legado.app.lib.mobi.entities

data class MobiEntryHeaders(
    val palmdoc: PalmDocHeader,
    val mobi: MobiHeader,
    val exth: Map<String, Any>,
    val kf8: KF8Header?
)
