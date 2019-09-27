package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.BookGroup

@Dao
interface BookGroupDao {

    @Query("SELECT * FROM book_groups ORDER BY `order`")
    fun liveDataAll(): LiveData<List<BookGroup>>

    @get:Query("SELECT MAX(groupId) FROM book_groups")
    val maxId: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookGroup: BookGroup)
}