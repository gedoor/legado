package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.SearchKeyword


@Dao
interface SearchKeywordDao {

    @get:Query("SELECT * FROM search_keywords")
    val all: List<SearchKeyword>

    @Query("SELECT * FROM search_keywords ORDER BY usage DESC")
    fun liveDataByUsage(): LiveData<List<SearchKeyword>>

    @Query("SELECT * FROM search_keywords ORDER BY lastUseTime DESC")
    fun liveDataByTime(): LiveData<List<SearchKeyword>>

    @Query("SELECT * FROM search_keywords where word like '%'||:key||'%' ORDER BY usage DESC")
    fun liveDataSearch(key: String): LiveData<List<SearchKeyword>>

    @Query("select * from search_keywords where word = :key")
    fun get(key: String): SearchKeyword?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg keywords: SearchKeyword)

    @Update
    fun update(vararg keywords: SearchKeyword)

    @Delete
    fun delete(vararg keywords: SearchKeyword)

    @Query("DELETE FROM search_keywords")
    fun deleteAll()

}