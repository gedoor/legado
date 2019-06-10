package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.BookGroup

@Dao
interface BookGroupDao {

    @Query("SELECT * FROM book_groups ORDER BY `order`")
    fun observeAll(): DataSource.Factory<Int, BookGroup>


}