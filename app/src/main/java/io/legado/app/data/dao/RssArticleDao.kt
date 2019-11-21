package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.RssArticle

@Dao
interface RssArticleDao {

    @Query("select * from rssArticles where origin = :origin and link = :link")
    fun get(origin: String, link: String): RssArticle?

    @Query("select * from rssArticles where origin = :origin order by `order` desc")
    fun liveByOrigin(origin: String): LiveData<List<RssArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssArticle: RssArticle)

    @Query("delete from rssArticles where origin = :origin and `order` < :order")
    fun clearOld(origin: String, order: Long)

    @Update
    fun update(vararg rssArticle: RssArticle)

    @Query("delete from rssArticles where origin = :origin")
    fun delete(origin: String)
}