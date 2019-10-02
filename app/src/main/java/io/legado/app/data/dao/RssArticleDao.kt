package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Query
import io.legado.app.data.entities.RssArticle

interface RssArticleDao {


    @Query("select * from rssArticles where origin = :origin")
    fun liveByOrigin(origin: String): LiveData<List<RssArticle>>


}