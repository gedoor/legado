package io.legado.app.model.rss

import io.legado.app.data.entities.RssArticle

data class Result(val articles: MutableList<RssArticle>, val nextPageUrl: String?)