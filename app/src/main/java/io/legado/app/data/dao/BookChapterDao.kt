package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.BookChapter

@Dao
interface BookChapterDao {

    @Query("select * from chapters where bookUrl = :bookUrl")
    fun observeByBook(bookUrl: String): DataSource.Factory<Int, BookChapter>

    @Query("select * from chapters where bookUrl = :bookUrl and `index` = :index")
    fun getChapter(bookUrl: String, index: Int): BookChapter?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookChapter: BookChapter)

}