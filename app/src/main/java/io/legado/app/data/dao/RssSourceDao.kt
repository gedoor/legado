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

    @Query("SELECT * FROM rssSources where sourceName like :key or sourceUrl like :key or sourceGroup like :key")
    fun liveSearch(key: String): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1")
    fun liveEnabled(): LiveData<List<RssSource>>

    @Query("select sourceGroup from rssSources where sourceGroup is not null and sourceGroup <> ''")
    fun liveGroup(): LiveData<List<String>>

    @Query("update rssSources set enabled = 1 where sourceUrl in (:sourceUrls)")
    fun enableSection(vararg sourceUrls: String)

    @Query("update rssSources set enabled = 0 where sourceUrl in (:sourceUrls)")
    fun disableSection(vararg sourceUrls: String)

    @Query("delete from rssSources where sourceUrl in (:sourceUrls)")
    fun delSection(vararg sourceUrls: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssSource: RssSource)

    @Update
    fun update(vararg rssSource: RssSource)

    @Query("delete from rssSources where sourceUrl = :sourceUrl")
    fun delete(sourceUrl: String)
}