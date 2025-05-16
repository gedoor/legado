package io.legado.app.utils

import io.legado.app.data.entities.RssArticle

fun RssArticle.updateVariableTo(rssArticle: RssArticle) {
    if (variable != rssArticle.variable) {
        rssArticle.variableMap.clear()
        rssArticle.variableMap.putAll(variableMap)
    }
}
