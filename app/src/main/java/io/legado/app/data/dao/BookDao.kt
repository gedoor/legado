package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.Book


@Dao
interface BookDao {

    @Query("SELECT * FROM books WHERE `group` = :group")
    fun observeByGroup(group: Int): DataSource.Factory<Int, Book>

    @Query("SELECT descUrl FROM books WHERE `group` = :group")
    fun observeUrlsByGroup(group: Int): LiveData<List<String>>

}