package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.cnCompare
import io.legado.app.utils.splitNotBlank
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Dao
interface RssSourceDao {

    @Query("select * from rssSources where sourceUrl = :key")
    fun getByKey(key: String): RssSource?

    @Query("select * from rssSources where sourceUrl in (:sourceUrls)")
    fun getRssSources(vararg sourceUrls: String): List<RssSource>

    @get:Query("SELECT * FROM rssSources order by customOrder")
    val all: List<RssSource>

    @get:Query("select count(sourceUrl) from rssSources")
    val size: Int

    @Query("SELECT * FROM rssSources order by customOrder")
    fun flowAll(): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources
        where sourceName like '%' || :key || '%' 
        or sourceUrl like '%' || :key || '%' 
        or sourceGroup like '%' || :key || '%'
        order by customOrder"""
    )
    fun flowSearch(key: String): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where (sourceGroup = :key
        or sourceGroup like :key || ',%' 
        or sourceGroup like  '%,' || :key
        or sourceGroup like  '%,' || :key || ',%')
        order by customOrder"""
    )
    fun flowGroupSearch(key: String): Flow<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1 order by customOrder")
    fun flowEnabled(): Flow<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 0 order by customOrder")
    fun flowDisabled(): Flow<List<RssSource>>

    @Query("select * from rssSources where loginUrl is not null and loginUrl != ''")
    fun flowLogin(): Flow<List<RssSource>>

    @Query("select * from rssSources where sourceGroup is null or sourceGroup = '' or sourceGroup like '%未分组%'")
    fun flowNoGroup(): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where enabled = 1 
        and (sourceName like '%' || :searchKey || '%' 
            or sourceGroup like '%' || :searchKey || '%' 
            or sourceUrl like '%' || :searchKey || '%') 
        order by customOrder"""
    )
    fun flowEnabled(searchKey: String): Flow<List<RssSource>>

    @Query(
        """SELECT * FROM rssSources 
        where enabled = 1 and (sourceGroup = :searchKey
        or sourceGroup like :searchKey || ',%' 
        or sourceGroup like  '%,' || :searchKey
        or sourceGroup like  '%,' || :searchKey || ',%') 
        order by customOrder"""
    )
    fun flowEnabledByGroup(searchKey: String): Flow<List<RssSource>>

    @Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> ''")
    fun flowGroupsUnProcessed(): Flow<List<String>>

    @Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> '' and enabled = 1")
    fun flowGroupEnabled(): Flow<List<String>>

    @get:Query("select distinct sourceGroup from rssSources where trim(sourceGroup) <> ''")
    val allGroupsUnProcessed: List<String>

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

    @Query("delete from rssSources where sourceGroup like 'legado'")
    fun deleteDefault()

    @get:Query("select * from rssSources where sourceGroup is null or sourceGroup = ''")
    val noGroup: List<RssSource>

    @Query("select * from rssSources where sourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<RssSource>

    @Query("select 1 from rssSources where sourceUrl = :key")
    fun has(key: String): Boolean?

    private fun dealGroups(list: List<String>): List<String> {
        val groups = linkedSetOf<String>()
        list.forEach {
            it.splitNotBlank(AppPattern.splitGroupRegex).forEach { group ->
                groups.add(group)
            }
        }
        return groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }
    }

    fun allGroups(): List<String> = dealGroups(allGroupsUnProcessed)

    fun flowGroups(): Flow<List<String>> {
        return flowGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }
}
