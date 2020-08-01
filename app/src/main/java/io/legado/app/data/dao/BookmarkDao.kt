package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.Bookmark


@Dao
interface BookmarkDao {

    @Query("select * from bookmarks")
    fun all(): List<Bookmark>

    @Query("select * from bookmarks where bookUrl = :bookUrl")
    fun observeByBook(bookUrl: String): DataSource.Factory<Int, Bookmark>

    @Query("SELECT * FROM bookmarks where bookUrl = :bookUrl and chapterName like '%'||:key||'%' or content like '%'||:key||'%'")
    fun liveDataSearch(bookUrl: String, key: String): DataSource.Factory<Int, Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookmark: Bookmark)

    @Delete
    fun delete(vararg bookmark: Bookmark)

    @Query("delete from bookmarks where bookUrl = :bookUrl and chapterName like '%'||:chapterName||'%'")
    fun delByBookmark(bookUrl: String, chapterName: String)

}