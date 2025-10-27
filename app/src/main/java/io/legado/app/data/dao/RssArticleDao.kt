package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.RssArticle
import kotlinx.coroutines.flow.Flow

@Dao
interface RssArticleDao {

    @Query("select * from rssArticles where origin = :origin and link = :link and sort = :sort")
    fun get(origin: String, link: String, sort: String): RssArticle?

    @Query("select * from rssArticles where origin = :origin and link = :link")
    fun getByLink(origin: String, link: String): RssArticle?

    @Query(
        """select t1.link, t1.sort, t1.origin, t1.`order`, t1.title, t1.content, 
            t1.description, t1.image, t1.`group`, t1.pubDate, t1.variable, t1.type, t1.durPos, ifNull(t2.read, 0) as read
        from rssArticles as t1 left join rssReadRecords as t2
        on t1.link = t2.record  where t1.origin = :origin and t1.sort = :sort
        order by `order` desc"""
    )
    fun flowByOriginSort(origin: String, sort: String): Flow<List<RssArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssArticle: RssArticle)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun append(vararg rssArticle: RssArticle)

    @Query("delete from rssArticles where origin = :origin and sort = :sort and `order` < :order")
    fun clearOld(origin: String, sort: String, order: Long)

    @Update
    fun update(vararg rssArticle: RssArticle)

    @Query("update rssArticles set origin = :origin where origin = :oldOrigin")
    fun updateOrigin(origin: String, oldOrigin: String)

    @Query("delete from rssArticles where origin = :origin")
    fun delete(origin: String)

}