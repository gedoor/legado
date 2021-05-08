package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.Bookmark


@Dao
interface BookmarkDao {

    @get:Query("select * from bookmarks")
    val all: List<Bookmark>

    @Query("select * from bookmarks where bookUrl = :bookUrl or (bookName = :bookName and bookAuthor = :bookAuthor)")
    fun observeByBook(
        bookUrl: String,
        bookName: String,
        bookAuthor: String
    ): LiveData<List<Bookmark>>

    @Query("SELECT * FROM bookmarks where bookUrl = :bookUrl and chapterName like '%'||:key||'%' or content like '%'||:key||'%'")
    fun liveDataSearch(bookUrl: String, key: String): LiveData<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookmark: Bookmark)

    @Update
    fun update(bookmark: Bookmark)

    @Delete
    fun delete(vararg bookmark: Bookmark)

    @Query("delete from bookmarks where bookUrl = :bookUrl and chapterName like '%'||:chapterName||'%'")
    fun delByBookmark(bookUrl: String, chapterName: String)

}