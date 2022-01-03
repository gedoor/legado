package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface RssArticleDao {

    @Query("select * from rssArticles where origin = :origin and link = :link")
    fun get(origin: String, link: String): RssArticle?

    @Query(
        """select t1.link, t1.sort, t1.origin, t1.`order`, t1.title, t1.content, 
            t1.description, t1.image, t1.pubDate, t1.variable, ifNull(t2.read, 0) as read
        from rssArticles as t1 left join rssReadRecords as t2
        on t1.link = t2.record  where origin = :origin and sort = :sort
        order by `order` desc"""
    )
    fun flowByOriginSort(origin: String, sort: String): Flow<List<RssArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssArticle: RssArticle)

    @Query("delete from rssArticles where origin = :origin and sort = :sort and `order` < :order")
    fun clearOld(origin: String, sort: String, order: Long)

    @Update
    fun update(vararg rssArticle: RssArticle)

    @Query("delete from rssArticles where origin = :origin")
    fun delete(origin: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRecord(vararg rssReadRecord: RssReadRecord)


}