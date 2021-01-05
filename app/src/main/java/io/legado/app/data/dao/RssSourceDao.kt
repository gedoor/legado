package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.RssSource

@Dao
interface RssSourceDao {

    @Query("select * from rssSources where sourceUrl = :key")
    fun getByKey(key: String): RssSource?

    @Query("select * from rssSources where sourceUrl in (:sourceUrls)")
    fun getRssSources(vararg sourceUrls: String): List<RssSource>

    @get:Query("SELECT * FROM rssSources")
    val all: List<RssSource>

    @get:Query("select count(sourceUrl) from rssSources")
    val size: Int

    @Query("SELECT * FROM rssSources order by customOrder")
    fun liveAll(): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where sourceName like :key or sourceUrl like :key or sourceGroup like :key order by customOrder")
    fun liveSearch(key: String): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where sourceGroup like :key order by customOrder")
    fun liveGroupSearch(key: String): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1 order by customOrder")
    fun liveEnabled(): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1 and (sourceName like :searchKey or sourceGroup like :searchKey or sourceUrl like :searchKey) order by customOrder")
    fun liveEnabled(searchKey: String): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1 and sourceGroup like :searchKey order by customOrder")
    fun liveEnabledByGroup(searchKey: String): LiveData<List<RssSource>>

    @Query("select sourceGroup from rssSources where trim(sourceGroup) <> ''")
    fun liveGroup(): LiveData<List<String>>

    @get:Query("select min(customOrder) from rssSources")
    val minOrder: Int

    @get:Query("select max(customOrder) from rssSources")
    val maxOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssSource: RssSource)

    @Update
    fun update(vararg rssSource: RssSource)

    @Delete
    fun delete(vararg rssSource: RssSource)

    @Query("delete from rssSources where sourceUrl = :sourceUrl")
    fun delete(sourceUrl: String)

    @get:Query("select * from rssSources where sourceGroup is null or sourceGroup = ''")
    val noGroup: List<RssSource>

    @Query("select * from rssSources where sourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<RssSource>
}