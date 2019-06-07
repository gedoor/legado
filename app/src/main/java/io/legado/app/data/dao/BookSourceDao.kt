package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from book_sources")
    fun observeAll(): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where origin = :key")
    fun findByKey(key:String): BookSource?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookSource: BookSource): Long

    @Update
    fun update(bookSource: BookSource)
}