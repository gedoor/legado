package io.legado.app.data.entities

data class SearchShow(
    var name: String = "",
    var author: String = "",
    override var kind: String? = null,
    var coverUrl: String? = null,
    var intro: String? = null,
    override var wordCount: String? = null,
    var latestChapterTitle: String? = null,
    var time: Long = 0L,
    var originCount: Int = 0
) : BaseBook {
    override var variableMap: HashMap<String, String>? = null

    override fun putVariable(key: String, value: String) = Unit
}