package io.legado.app.data.entities.rule

data class BookInfoRule(
    var urlPattern: String? = null,
    var init: String? = null,
    var name: String? = null,
    var author: String? = null,
    var desc: String? = null,
    var meta: String? = null,
    var lastChapter: String? = null,
    var updateTime: String? = null,
    var coverUrl: String? = null,
    var tocUrl: String? = null
)