package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.BookGroup

@Dao
interface BookGroupDao {

    @Query("SELECT * FROM book_groups ORDER BY `order`")
    fun observeAll(): DataSource.Factory<Int, BookGroup>

    @get:Query("SELECT MAX(groupId) FROM book_groups")
    val maxId: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookGroup: BookGroup)
}