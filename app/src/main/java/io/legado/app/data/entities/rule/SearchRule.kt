package io.legado.app.data.entities.rule

data class SearchRule(
    var bookList: Rule,
    var name: Rule,
    var author: Rule,
    var desc: Rule,
    var meta: Rule,
    var bookUrl: Rule,
    var store: List<PutRule>
)