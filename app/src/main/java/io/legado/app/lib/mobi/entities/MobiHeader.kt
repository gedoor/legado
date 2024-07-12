package io.legado.app.lib.mobi.entities

data class MobiHeader(
    val identifier: String,
    val length: Int,
    val type: Int,
    val encoding: Int,
    val uid: Int,
    val version: Int,
    val titleOffset: Int,
    val titleLength: Int,
    val localeRegion: Int,
    val localeLanguage: Int,
    val resourceStart: Int,
    val huffcdic: Int,
    val numHuffcdic: Int,
    val exthFlag: Int,
    val trailingFlags: Int,
    val indx: Int,
    val title: String,
    val languege: String
)
