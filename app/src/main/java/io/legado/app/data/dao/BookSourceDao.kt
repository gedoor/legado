package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun observeAll(): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where origin = :key")
    fun findByKey(key:String): BookSource?

    @Query("select count(*) from book_sources")
    fun allCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookSource: BookSource): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookSource: BookSource)

    @Update
    fun update(bookSource: BookSource)

    @Update
    fun update(vararg bookSource: BookSource)

    @Delete
    fun delete(bookSource: BookSource)

    @Delete
    fun delete(vararg bookSource: BookSource)
}