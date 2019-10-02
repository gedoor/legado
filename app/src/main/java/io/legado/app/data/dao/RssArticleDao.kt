package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.RssArticle

interface RssArticleDao {


    @Query("select * from rssArticles where origin = :origin order by time desc")
    fun liveByOrigin(origin: String): LiveData<List<RssArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssArticle: RssArticle)
}