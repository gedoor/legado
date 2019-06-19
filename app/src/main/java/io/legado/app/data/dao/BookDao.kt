package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT descUrl FROM books WHERE `group` = :group")
    fun observeUrlsByGroup(group: Int): LiveData<List<String>>

    @Query("SELECT * FROM books WHERE `name` in (:names)")
    fun findByName(vararg names: String): List<Book>

    @get:Query("SELECT descUrl FROM books")
    val allBookUrls: List<String>

    @get:Query("SELECT COUNT(*) FROM books")
    val allBookCount: Int

    @Query("SELECT * FROM books ORDER BY durChapterTime DESC limit 0,10")
    fun recentRead(): DataSource.Factory<Int, Book>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg books: Book)

}