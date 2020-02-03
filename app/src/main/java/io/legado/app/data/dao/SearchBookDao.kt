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

    @Query("select * from searchBooks where name = :name and author = :author order by originOrder limit 1")
    fun getFirstByNameAuthor(name: String, author: String): SearchBook?

    @Query("select * from searchBooks where name = :name and author = :author order by originOrder")
    fun getByNameAuthor(name: String, author: String): List<SearchBook>

    @Query("select * from searchBooks where name = :name and author = :author and origin in (select bookSourceUrl from book_sources where enabled = 1) order by originOrder")
    fun getByNameAuthorEnable(name: String, author: String): List<SearchBook>

    @Query(
        """select * from searchBooks
                     where name = :name and author = :author and origin in (select bookSourceUrl from book_sources where enabled = 1)
                         and coverUrl is not null and coverUrl <> ''
                     order by originOrder"""
    )
    fun getEnableHasCover(name: String, author: String): List<SearchBook>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg searchBook: SearchBook): List<Long>

}