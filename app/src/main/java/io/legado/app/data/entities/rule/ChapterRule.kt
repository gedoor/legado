package io.legado.app.data.entities.rule

data class ChapterRule (
    var chapterList: Rule,
    var isReversed: Boolean = false,
    var title: Rule,
    var contentUrl: Rule,
    var resourceUrl: Rule,
    var nextUrl: Rule
)