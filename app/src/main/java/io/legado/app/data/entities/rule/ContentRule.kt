package io.legado.app.data.entities.rule

data class ContentRule(
    var content: String? = null,
    var nextContentUrl: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null
)