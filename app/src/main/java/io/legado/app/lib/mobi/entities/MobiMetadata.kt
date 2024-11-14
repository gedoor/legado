package io.legado.app.lib.mobi.entities

data class MobiMetadata(
    val identifier: String,
    val title: String,
    val author: List<String>,
    val publisher: String,
    val language: String,
    val published: String,
    val description: String,
    val subject: List<String>,
    val rights: String
)
