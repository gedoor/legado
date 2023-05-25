package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.Bookmark
import kotlinx.coroutines.flow.Flow


@Dao
interface BookmarkDao {

    @get:Query(
        """
        select * from bookmarks order by bookName collate localized, bookAuthor collate localized, chapterIndex, chapterPos
    """
    )
    val all: List<Bookmark>

    @Query("select * from bookmarks order by bookName collate localized, bookAuthor collate localized, chapterIndex, chapterPos")
    fun flowAll(): Flow<List<Bookmark>>

    @Query(
        """select * from bookmarks 
        where bookName = :bookName and bookAuthor = :bookAuthor 
        order by chapterIndex"""
    )
    fun flowByBook(bookName: String, bookAuthor: String): Flow<List<Bookmark>>

    @Query(
        """SELECT * FROM bookmarks 
        where bookName = :bookName and bookAuthor = :bookAuthor 
        and chapterName like '%'||:key||'%' or content like '%'||:key||'%'
        order by chapterIndex"""
    )
    fun flowSearch(bookName: String, bookAuthor: String, key: String): Flow<List<Bookmark>>

    @Query(
        """select * from bookmarks 
        where bookName = :bookName and bookAuthor = :bookAuthor 
        order by chapterIndex"""
    )
    fun getByBook(bookName: String, bookAuthor: String): List<Bookmark>

    @Query(
        """SELECT * FROM bookmarks 
        where bookName = :bookName and bookAuthor = :bookAuthor 
        and chapterName like '%'||:key||'%' or content like '%'||:key||'%'
        order by chapterIndex"""
    )
    fun search(bookName: String, bookAuthor: String, key: String): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookmark: Bookmark)

    @Update
    fun update(bookmark: Bookmark)

    @Delete
    fun delete(vararg bookmark: Bookmark)

}