package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.constant.DbTable
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from ${DbTable.book_sources}")
    fun observeAll(): DataSource.Factory<Int, BookSource>
}