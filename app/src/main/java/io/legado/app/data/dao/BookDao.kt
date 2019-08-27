package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    fun observeAll(): DataSource.Factory<Int, Book>

    @Query("SELECT * FROM books WHERE type = ${BookType.audio}")
    fun observeAudio(): DataSource.Factory<Int, Book>

    @Query("SELECT * FROM books WHERE origin = '${BookType.local}'")
    fun observeLocal(): DataSource.Factory<Int, Book>

    @Query("SELECT * FROM books WHERE `group` = :group")
    fun observeByGroup(group: Int): DataSource.Factory<Int, Book>

    @Query("SELECT bookUrl FROM books WHERE `group` = :group")
    fun observeUrlsByGroup(group: Int): LiveData<List<String>>

    @Query("SELECT * FROM books WHERE `name` in (:names)")
    fun findByName(vararg names: String): List<Book>

    @Query("SELECT * FROM books WHERE bookUrl = :bookUrl")
    fun getBook(bookUrl: String): Book?

    @get:Query("SELECT * FROM books ORDER BY durChapterTime DESC limit 1")
    val lastReadBook: Book?

    @get:Query("SELECT bookUrl FROM books")
    val allBookUrls: List<String>

    @get:Query("SELECT COUNT(*) FROM books")
    val allBookCount: Int

    @Query("SELECT * FROM books ORDER BY durChapterTime DESC limit 0,20")
    fun recentRead(): DataSource.Factory<Int, Book>

    @Query("SELECT * FROM books ORDER BY durChapterTime DESC limit 0,20")
    fun getRecentRead(): List<Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg book: Book)

    @Update
    fun update(vararg books: Book)

    @Query("delete from books where bookUrl = :bookUrl")
    fun delete(bookUrl: String)
}