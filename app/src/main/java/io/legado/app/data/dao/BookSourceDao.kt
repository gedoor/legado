package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.cnCompare
import io.legado.app.utils.splitNotBlank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun flowAll(): Flow<List<BookSource>>

    @Query(
        """select * from book_sources 
        where bookSourceName like '%' || :searchKey || '%'
        or bookSourceGroup like '%' || :searchKey || '%'
        or bookSourceUrl like '%' || :searchKey || '%'
        or bookSourceComment like '%' || :searchKey || '%' 
        order by customOrder asc"""
    )
    fun flowSearch(searchKey: String): Flow<List<BookSource>>

    @Query(
        """select * from book_sources 
        where enabled = 1 and 
        (bookSourceName like '%' || :searchKey || '%' 
        or bookSourceGroup like '%' || :searchKey || '%' 
        or bookSourceUrl like '%' || :searchKey || '%'  
        or bookSourceComment like '%' || :searchKey || '%')
        order by customOrder asc"""
    )
    fun flowSearchEnabled(searchKey: String): Flow<List<BookSource>>

    @Query(
        """select * from book_sources 
        where bookSourceGroup = :searchKey
        or bookSourceGroup like :searchKey || ',%' 
        or bookSourceGroup like  '%,' || :searchKey
        or bookSourceGroup like  '%,' || :searchKey || ',%' 
        order by customOrder asc"""
    )
    fun flowGroupSearch(searchKey: String): Flow<List<BookSource>>

    @Query("select * from book_sources where enabled = 1 order by customOrder asc")
    fun flowEnabled(): Flow<List<BookSource>>

    @Query("select * from book_sources where enabled = 0 order by customOrder asc")
    fun flowDisabled(): Flow<List<BookSource>>

    @Query("select * from book_sources where enabledExplore = 1 and trim(exploreUrl) <> '' order by customOrder asc")
    fun flowExplore(): Flow<List<BookSource>>

//    @Query("select * from book_sources where enabledReview = 1 order by customOrder asc")
//    fun flowReview(): Flow<List<BookSource>>

    @Query("select * from book_sources where loginUrl is not null and loginUrl != ''")
    fun flowLogin(): Flow<List<BookSource>>

    @Query("select * from book_sources where bookSourceGroup is null or bookSourceGroup = '' or bookSourceGroup like '%未分组%'")
    fun flowNoGroup(): Flow<List<BookSource>>

    @Query(
        """select * from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup like '%' || :key || '%' 
            or bookSourceName like '%' || :key || '%') 
        order by customOrder asc"""
    )
    fun flowExplore(key: String): Flow<List<BookSource>>

    @Query(
        """select * from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup = :key
            or bookSourceGroup like :key || ',%' 
            or bookSourceGroup like  '%,' || :key
            or bookSourceGroup like  '%,' || :key || ',%') 
        order by customOrder asc"""
    )
    fun flowGroupExplore(key: String): Flow<List<BookSource>>

    @Query("select distinct bookSourceGroup from book_sources where trim(bookSourceGroup) <> ''")
    fun flowGroupsUnProcessed(): Flow<List<String>>

    @Query("select distinct bookSourceGroup from book_sources where enabled = 1 and trim(bookSourceGroup) <> ''")
    fun flowEnabledGroupsUnProcessed(): Flow<List<String>>

    @Query(
        """select distinct bookSourceGroup from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and trim(bookSourceGroup) <> ''
        order by customOrder"""
    )
    fun flowExploreGroupsUnProcessed(): Flow<List<String>>

    @Query("select * from book_sources where bookSourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<BookSource>

    @Query(
        """select * from book_sources 
        where enabled = 1 
        and (bookSourceGroup = :group
            or bookSourceGroup like :group || ',%' 
            or bookSourceGroup like  '%,' || :group
            or bookSourceGroup like  '%,' || :group || ',%')"""
    )
    fun getEnabledByGroup(group: String): List<BookSource>

    @Query("select * from book_sources where enabled = 1 and bookSourceType = :type")
    fun getEnabledByType(type: Int): List<BookSource>

    @get:Query("select * from book_sources where enabled = 1 and trim(bookUrlPattern) <> '' order by enabled desc, customOrder")
    val hasBookUrlPattern: List<BookSource>

    @get:Query("select * from book_sources where bookSourceGroup is null or bookSourceGroup = ''")
    val noGroup: List<BookSource>

    @get:Query("select * from book_sources order by customOrder asc")
    val all: List<BookSource>

    @get:Query("select * from book_sources where enabled = 1 order by customOrder")
    val allEnabled: List<BookSource>

    @get:Query("select * from book_sources where enabled = 1 and bookSourceType = 0 order by customOrder")
    val allTextEnabled: List<BookSource>

    @get:Query("select distinct bookSourceGroup from book_sources where trim(bookSourceGroup) <> ''")
    val allGroupsUnProcessed: List<String>

    @get:Query("select distinct bookSourceGroup from book_sources where enabled = 1 and trim(bookSourceGroup) <> ''")
    val allEnabledGroupsUnProcessed: List<String>

    @Query("select * from book_sources where bookSourceUrl = :key")
    fun getBookSource(key: String): BookSource?

    @Query("select count(*) from book_sources")
    fun allCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookSource: BookSource)

    @Update
    fun update(vararg bookSource: BookSource)

    @Delete
    fun delete(vararg bookSource: BookSource)

    @Query("delete from book_sources where bookSourceUrl = :key")
    fun delete(key: String)

    @get:Query("select min(customOrder) from book_sources")
    val minOrder: Int

    @get:Query("select max(customOrder) from book_sources")
    val maxOrder: Int

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

    val allGroups: List<String>
        get() {
            return dealGroups(allGroupsUnProcessed)
        }

    val allEnabledGroups: List<String>
        get() {
            return dealGroups(allEnabledGroupsUnProcessed)
        }

    fun flowGroups(): Flow<List<String>> {
        return flowGroupsUnProcessed().map { list ->
            dealGroups(list)
        }
    }

    fun flowExploreGroups(): Flow<List<String>> {
        return flowExploreGroupsUnProcessed().map { list ->
            dealGroups(list)
        }
    }

    fun flowEnabledGroups(): Flow<List<String>> {
        return flowEnabledGroupsUnProcessed().map { list ->
            dealGroups(list)
        }
    }
}