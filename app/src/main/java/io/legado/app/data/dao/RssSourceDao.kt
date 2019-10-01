package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.RssSource

@Dao
interface RssSourceDao {

    @Query("select * from rssSources where sourceUrl = :key")
    fun getByKey(key: String): RssSource?

    @get:Query("SELECT * FROM rssSources")
    val all: List<RssSource>

    @Query("SELECT * FROM rssSources")
    fun liveAll(): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1")
    fun liveEnabled(): LiveData<List<RssSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssSource: RssSource)

    @Update
    fun update(vararg rssSource: RssSource)

    @Query("delete from rssSources where sourceUrl = :sourceUrl")
    fun delete(sourceUrl: String)
}