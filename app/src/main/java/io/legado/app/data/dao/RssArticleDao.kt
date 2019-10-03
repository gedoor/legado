package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.RssArticle

@Dao
interface RssArticleDao {

    @Query("select * from rssArticles where origin = :origin order by time desc")
    fun liveByOrigin(origin: String): LiveData<List<RssArticle>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(vararg rssArticle: RssArticle)

    @Update
    fun update(vararg rssArticle: RssArticle)
}