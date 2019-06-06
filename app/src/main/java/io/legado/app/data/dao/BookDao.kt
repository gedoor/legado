package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.constant.DbTable
import io.legado.app.data.entities.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM ${DbTable.books} WHERE `group` = :group")
    fun observeByGroup(group: Int): DataSource.Factory<Int, Book>

    @Query("SELECT descUrl FROM ${DbTable.books} WHERE `group` = :group")
    fun observeUrlsByGroup(group: Int): LiveData<List<String>>

    @Query("SELECT * FROM ${DbTable.books} WHERE `name` in (:names)")
    fun findByName(vararg names: String): List<Book>

    @get:Query("SELECT descUrl FROM ${DbTable.books}")
    val allBookUrls: List<String>

    @get:Query("SELECT COUNT(*) FROM ${DbTable.books}")
    val allBookCount: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg books: Book)

}