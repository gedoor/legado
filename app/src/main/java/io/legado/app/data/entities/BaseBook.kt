package io.legado.app.data.entities

interface BaseBook {
    var variableMap: HashMap<String, String>?
    fun putVariable(key: String, value: String)
}