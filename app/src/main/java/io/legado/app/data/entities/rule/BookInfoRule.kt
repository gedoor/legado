package io.legado.app.data.entities.rule

data class BookInfoRule (
    var name: Rule,
    var author: Rule,
    var desc: Rule,
    var meta: Rule,
    var updateTime: Rule,
    var tocUrl: Rule,
    var store: List<PutRule>
)