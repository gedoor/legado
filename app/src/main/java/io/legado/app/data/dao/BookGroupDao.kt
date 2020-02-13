package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.BookGroup

@Dao
interface BookGroupDao {

    @Query("select * from book_groups where groupId = :id")
    fun getByID(id: Int): BookGroup?

    @Query("SELECT * FROM book_groups ORDER BY `order`")
    fun liveDataAll(): LiveData<List<BookGroup>>

    @get:Query("SELECT sum(groupId) FROM book_groups")
    val idsSum: Int

    @get:Query("SELECT MAX(`order`) FROM book_groups")
    val maxOrder: Int

    @get:Query("SELECT * FROM book_groups ORDER BY `order`")
    val all: List<BookGroup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookGroup: BookGroup)

    @Update
    fun update(vararg bookGroup: BookGroup)

    @Delete
    fun delete(vararg bookGroup: BookGroup)
}