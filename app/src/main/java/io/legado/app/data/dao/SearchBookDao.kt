package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.SearchBook

@Dao
interface SearchBookDao {

    @Query("SELECT * FROM searchBooks")
    fun observeAll(): DataSource.Factory<Int, SearchBook>

    @Query("SELECT * FROM searchBooks where time >= :time")
    fun observeNew(time: Long): DataSource.Factory<Int, SearchBook>

    @Query("select * from searchBooks where bookUrl = :bookUrl")
    fun getSearchBook(bookUrl: String): SearchBook?

    @Query("select * from searchBooks where name = :name and author = :author and origin in (select bookSourceUrl from book_sources) order by originOrder limit 1")
    fun getFirstByNameAuthor(name: String, author: String): SearchBook?

    @Query(
        """
        select t1.name, t1.author, t1.origin, t1.originName, t1.coverUrl, t1.bookUrl, t1.type, t1.time, t1.intro, t1.kind, t1.latestChapterTitle, t1.tocUrl, t1.variable, t1.wordCount, t2.customOrder as originOrder
        from searchBooks as t1 inner join book_sources as t2 
        on t1.origin = t2.bookSourceUrl 
        where t1.name = :name and t1.author like '%'||:author||'%' and t2.enabled = 1 and t2.bookSourceGroup like '%'||:sourceGroup||'%'
        order by t2.customOrder
        """
    )
    fun getChangeSourceSearch(name: String, author: String, sourceGroup: String): List<SearchBook>

    @Query(
        """
        select t1.name, t1.author, t1.origin, t1.originName, t1.coverUrl, t1.bookUrl, t1.type, t1.time, t1.intro, t1.kind, t1.latestChapterTitle, t1.tocUrl, t1.variable, t1.wordCount, t2.customOrder as originOrder
        from searchBooks as t1 inner join book_sources as t2 
        on t1.origin = t2.bookSourceUrl 
        where t1.name = :name and t1.author = :author and originName like '%'||:key||'%' and t2.enabled = 1 and t2.bookSourceGroup like '%'||:sourceGroup||'%'
        order by t2.customOrder
        """
    )
    fun getChangeSourceSearch(
        name: String,
        author: String,
        key: String,
        sourceGroup: String
    ): List<SearchBook>

    @Query(
        """
        select t1.name, t1.author, t1.origin, t1.originName, t1.coverUrl, t1.bookUrl, t1.type, t1.time, t1.intro, t1.kind, t1.latestChapterTitle, t1.tocUrl, t1.variable, t1.wordCount, t2.customOrder as originOrder
        from searchBooks as t1 inner join book_sources as t2 
        on t1.origin = t2.bookSourceUrl 
        where t1.name = :name and t1.author = :author and t1.coverUrl is not null and t1.coverUrl <> '' and t2.enabled = 1
        order by t2.customOrder
        """
    )
    fun getEnableHasCover(name: String, author: String): List<SearchBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg searchBook: SearchBook): List<Long>

    @Query("delete from searchBooks where name = :name and author = :author")
    fun clear(name: String, author: String)

    @Query("delete from searchBooks where time < :time")
    fun clearExpired(time: Long)

}