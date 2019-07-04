package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import io.legado.app.data.entities.BookChapter

@Dao
interface BookChapterDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookChapter: BookChapter)

}