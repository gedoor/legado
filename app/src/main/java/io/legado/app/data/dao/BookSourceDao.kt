package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.BookSource
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun liveDataAll(): Flow<List<BookSource>>

    @Query(
        """
        select * from book_sources 
        where bookSourceName like :searchKey 
        or bookSourceGroup like :searchKey 
        or bookSourceUrl like :searchKey  
        or bookSourceComment like :searchKey 
        order by customOrder asc
        """
    )
    fun liveDataSearch(searchKey: String): Flow<List<BookSource>>

    @Query("select * from book_sources where bookSourceGroup like :searchKey order by customOrder asc")
    fun liveDataGroupSearch(searchKey: String): Flow<List<BookSource>>

    @Query("select * from book_sources where enabled = 1 order by customOrder asc")
    fun liveDataEnabled(): Flow<List<BookSource>>

    @Query("select * from book_sources where enabled = 0 order by customOrder asc")
    fun liveDataDisabled(): Flow<List<BookSource>>

    @Query("select * from book_sources where enabledExplore = 1 and trim(exploreUrl) <> '' order by customOrder asc")
    fun liveExplore(): Flow<List<BookSource>>

    @Query(
        """
        select * from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup like :key or bookSourceName like :key) 
        order by customOrder asc
        """
    )
    fun liveExplore(key: String): Flow<List<BookSource>>

    @Query(
        """
        select * from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup like :key) 
        order by customOrder asc
        """
    )
    fun liveGroupExplore(key: String): Flow<List<BookSource>>

    @Query("select distinct bookSourceGroup from book_sources where trim(bookSourceGroup) <> ''")
    fun liveGroup(): Flow<List<String>>

    @Query("select distinct bookSourceGroup from book_sources where enabled = 1 and trim(bookSourceGroup) <> ''")
    fun liveGroupEnabled(): Flow<List<String>>

    @Query(
        """
        select distinct bookSourceGroup from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and trim(bookSourceGroup) <> ''
        """
    )
    fun liveExploreGroup(): Flow<List<String>>

    @Query("select * from book_sources where bookSourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<BookSource>

    @Query("select * from book_sources where enabled = 1 and bookSourceGroup like '%' || :group || '%'")
    fun getEnabledByGroup(group: String): List<BookSource>

    @get:Query("select * from book_sources where trim(bookUrlPattern) <> ''")
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
    val allGroup: List<String>

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
}