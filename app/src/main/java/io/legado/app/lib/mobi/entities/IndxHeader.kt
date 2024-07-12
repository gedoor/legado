package io.legado.app.lib.mobi.entities

data class IndxHeader(
    val magic: String,
    val length: Int,
    val type: Int,
    val idxt: Int,
    val numRecords: Int,
    val encoding: Int,
    val language: Int,
    val total: Int,
    val ordt: Int,
    val ligt: Int,
    val numLigt: Int,
    val numCncx: Int,
)
