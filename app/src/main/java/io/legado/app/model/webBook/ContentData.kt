package io.legado.app.model.webBook

data class ContentData<T>(
    var content: String = "",
    var nextUrl: T
)