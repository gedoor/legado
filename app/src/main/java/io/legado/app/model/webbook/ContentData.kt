package io.legado.app.model.webbook

data class ContentData<T>(
    var content: String,
    var nextUrl: T
)