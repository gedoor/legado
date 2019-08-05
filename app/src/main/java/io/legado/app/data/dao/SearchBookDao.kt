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


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg searchBook: SearchBook): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchBook: SearchBook): Long
}