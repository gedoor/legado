package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.IdealEntity

@Dao
interface BookIdealDao {

    /**
     * 根据书名查找想法列表
     */
    @Query("SELECT * FROM book_ideals WHERE `bookName` = bookName")
    fun getIdealList(bookName: String): LiveData<List<IdealEntity>>
}