package io.legado.app.data.entities

import io.legado.app.utils.splitNotBlank

interface BaseBook {
    var name: String
    var author: String
    var bookUrl: String
    val variableMap: HashMap<String, String>
    var kind: String?
    var wordCount: String?

    var infoHtml: String?
    var tocHtml: String?

    fun putVariable(key: String, value: String) {}

    fun getKindList(): List<String> {
        val kindList = arrayListOf<String>()
        wordCount?.let {
            if (it.isNotBlank()) kindList.add(it)
        }
        kind?.let {
            val kinds = it.splitNotBlank(",", "\n")
            kindList.addAll(kinds)
        }
        return kindList
    }
}