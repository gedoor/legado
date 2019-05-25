package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.SearchKeyword


@Dao
interface SearchKeywordDao {

    @Query("SELECT * FROM search_keywords ORDER BY usage DESC")
    fun observeByUsage(): DataSource.Factory<Int, SearchKeyword>

    @Query("SELECT * FROM search_keywords ORDER BY lastUseTime DESC")
    fun observeByTime(): DataSource.Factory<Int, SearchKeyword>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg keywords: SearchKeyword)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(keyword: SearchKeyword): Long

    @Update
    fun update(vararg keywords: SearchKeyword)

    @Delete
    fun delete(vararg keywords: SearchKeyword)

    @Query("DELETE FROM search_keywords")
    fun deleteAll()

}