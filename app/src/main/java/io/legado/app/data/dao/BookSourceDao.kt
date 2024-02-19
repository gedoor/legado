package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.legado.app.constant.AppPattern
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.utils.cnCompare
import io.legado.app.utils.splitNotBlank
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@Dao
interface BookSourceDao {

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources order by customOrder asc"""
    )
    fun flowAll(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where bookSourceName like '%' || :searchKey || '%'
        or bookSourceGroup like '%' || :searchKey || '%'
        or bookSourceUrl like '%' || :searchKey || '%'
        or bookSourceComment like '%' || :searchKey || '%' 
        order by customOrder asc"""
    )
    fun flowSearch(searchKey: String): Flow<List<BookSourcePart>>

    @Query(
        """select * from book_sources 
        where bookSourceName like '%' || :searchKey || '%'
        or bookSourceGroup like '%' || :searchKey || '%'
        or bookSourceUrl like '%' || :searchKey || '%'
        or bookSourceComment like '%' || :searchKey || '%' 
        order by customOrder asc"""
    )
    fun search(searchKey: String): List<BookSource>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where enabled = 1 and 
        (bookSourceName like '%' || :searchKey || '%' 
        or bookSourceGroup like '%' || :searchKey || '%' 
        or bookSourceUrl like '%' || :searchKey || '%'  
        or bookSourceComment like '%' || :searchKey || '%')
        order by customOrder asc"""
    )
    fun flowSearchEnabled(searchKey: String): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where bookSourceGroup = :searchKey
        or bookSourceGroup like :searchKey || ',%' 
        or bookSourceGroup like  '%,' || :searchKey
        or bookSourceGroup like  '%,' || :searchKey || ',%' 
        order by customOrder asc"""
    )
    fun flowGroupSearch(searchKey: String): Flow<List<BookSourcePart>>

    @Query(
        """select * from book_sources 
        where bookSourceGroup = :searchKey
        or bookSourceGroup like :searchKey || ',%' 
        or bookSourceGroup like  '%,' || :searchKey
        or bookSourceGroup like  '%,' || :searchKey || ',%' 
        order by customOrder asc"""
    )
    fun groupSearch(searchKey: String): List<BookSource>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabled = 1 order by customOrder asc"""
    )
    fun flowEnabled(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabled = 0 order by customOrder asc"""
    )
    fun flowDisabled(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabledExplore = 1 and trim(exploreUrl) <> '' order by customOrder asc"""
    )
    fun flowExplore(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where loginUrl is not null and loginUrl != ''
        order by customOrder asc"""
    )
    fun flowLogin(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where bookSourceGroup is null or bookSourceGroup = '' or bookSourceGroup like '%未分组%'
        order by customOrder asc"""
    )
    fun flowNoGroup(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabledExplore = 1 order by customOrder asc"""
    )
    fun flowEnabledExplore(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabledExplore = 0 order by customOrder asc"""
    )
    fun flowDisabledExplore(): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup like '%' || :key || '%' 
            or bookSourceName like '%' || :key || '%') 
        order by customOrder asc"""
    )
    fun flowExplore(key: String): Flow<List<BookSourcePart>>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where enabledExplore = 1 
        and trim(exploreUrl) <> '' 
        and (bookSourceGroup = :key
            or bookSourceGroup like :key || ',%' 
            or bookSourceGroup like  '%,' || :key
            or bookSourceGroup like  '%,' || :key || ',%') 
        order by customOrder asc"""
    )
    fun flowGroupExplore(key: String): Flow<List<BookSourcePart>>

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

    @Query("select * from book_sources where bookSourceGroup like '%' || :group || '%' order by customOrder asc")
    fun getByGroup(group: String): List<BookSource>

    @Query(
        """select * from book_sources 
        where enabled = 1 
        and (bookSourceGroup = :group
            or bookSourceGroup like :group || ',%' 
            or bookSourceGroup like  '%,' || :group
            or bookSourceGroup like  '%,' || :group || ',%')
        order by customOrder asc"""
    )
    fun getEnabledByGroup(group: String): List<BookSource>

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources 
        where enabled = 1 
        and (bookSourceGroup = :group
            or bookSourceGroup like :group || ',%' 
            or bookSourceGroup like  '%,' || :group
            or bookSourceGroup like  '%,' || :group || ',%')
        order by customOrder asc"""
    )
    fun getEnabledPartByGroup(group: String): List<BookSourcePart>

    @Query("select * from book_sources where bookUrlPattern != 'NONE' and bookSourceType = :type order by customOrder asc")
    fun getEnabledByType(type: Int): List<BookSource>

    @Query("select * from book_sources where enabled = 1 and bookSourceUrl = :baseUrl")
    fun getBookSourceAddBook(baseUrl: String): BookSource?

    @get:Query("select * from book_sources where enabled = 1 and trim(bookUrlPattern) <> '' and trim(bookUrlPattern) <> 'NONE' order by customOrder")
    val hasBookUrlPattern: List<BookSource>

    @get:Query("select * from book_sources where bookSourceGroup is null or bookSourceGroup = ''")
    val noGroup: List<BookSource>

    @get:Query("select * from book_sources order by customOrder asc")
    val all: List<BookSource>

    @get:Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources order by customOrder asc"""
    )
    val allPart: List<BookSourcePart>

    @get:Query("select * from book_sources where enabled = 1 order by customOrder")
    val allEnabled: List<BookSource>

    @get:Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabled = 1 order by customOrder asc"""
    )
    val allEnabledPart: List<BookSourcePart>

    @get:Query("select * from book_sources where enabled = 0 order by customOrder")
    val allDisabled: List<BookSource>

    @get:Query("select * from book_sources where bookSourceGroup is null or bookSourceGroup = '' or bookSourceGroup like '%未分组%'")
    val allNoGroup: List<BookSource>

    @get:Query("select * from book_sources where enabledExplore = 1 order by customOrder")
    val allEnabledExplore: List<BookSource>

    @get:Query("select * from book_sources where enabledExplore = 0 order by customOrder")
    val allDisabledExplore: List<BookSource>

    @get:Query("select * from book_sources where loginUrl is not null and loginUrl != ''")
    val allLogin: List<BookSource>

    @get:Query(
        """select  bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where enabled = 1 and bookSourceType = 0 order by customOrder"""
    )
    val allTextEnabledPart: List<BookSourcePart>

    @get:Query("select distinct bookSourceGroup from book_sources where trim(bookSourceGroup) <> ''")
    val allGroupsUnProcessed: List<String>

    @get:Query("select distinct bookSourceGroup from book_sources where enabled = 1 and trim(bookSourceGroup) <> ''")
    val allEnabledGroupsUnProcessed: List<String>

    @Query("select * from book_sources where bookSourceUrl = :key")
    fun getBookSource(key: String): BookSource?

    @Query(
        """select bookSourceUrl, bookSourceName, bookSourceGroup, customOrder, enabled, enabledExplore,
        trim(loginUrl) <> '' hasLoginUrl, lastUpdateTime, respondTime, weight, trim(exploreUrl) <> '' hasExploreUrl
        from book_sources where bookSourceUrl = :key"""
    )
    fun getBookSourcePart(key: String): BookSourcePart?

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

    @Transaction
    fun delete(bookSources: List<BookSourcePart>) {
        for (bs in bookSources) {
            delete(bs.bookSourceUrl)
        }
    }

    @get:Query("select min(customOrder) from book_sources")
    val minOrder: Int

    @get:Query("select max(customOrder) from book_sources")
    val maxOrder: Int

    @Query("update book_sources set enabled = :enable where bookSourceUrl = :bookSourceUrl")
    fun enable(bookSourceUrl: String, enable: Boolean)

    @Transaction
    fun enable(enable: Boolean, bookSources: List<BookSourcePart>) {
        for (bs in bookSources) {
            enable(bs.bookSourceUrl, enable)
        }
    }

    @Query("update book_sources set enabledExplore = :enable where bookSourceUrl = :bookSourceUrl")
    fun enableExplore(bookSourceUrl: String, enable: Boolean)

    @Transaction
    fun enableExplore(enable: Boolean, bookSources: List<BookSourcePart>) {
        for (bs in bookSources) {
            enableExplore(bs.bookSourceUrl, enable)
        }
    }

    @Query("update book_sources set customOrder = :customOrder where bookSourceUrl = :bookSourceUrl")
    fun upOrder(bookSourceUrl: String, customOrder: Int)

    @Transaction
    fun upOrder(bookSources: List<BookSourcePart>) {
        for (bs in bookSources) {
            upOrder(bs.bookSourceUrl, bs.customOrder)
        }
    }

    fun upOrder(bookSource: BookSourcePart) {
        upOrder(bookSource.bookSourceUrl, bookSource.customOrder)
    }

    @Query("update book_sources set bookSourceGroup = :bookSourceGroup where bookSourceUrl = :bookSourceUrl")
    fun upGroup(bookSourceUrl: String, bookSourceGroup: String)

    @Transaction
    fun upGroup(bookSources: List<BookSourcePart>) {
        for (bs in bookSources) {
            bs.bookSourceGroup?.let { upGroup(bs.bookSourceUrl, it) }
        }
    }

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

    fun allEnabledGroups(): List<String> = dealGroups(allEnabledGroupsUnProcessed)

    fun flowGroups(): Flow<List<String>> {
        return flowGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }

    fun flowExploreGroups(): Flow<List<String>> {
        return flowExploreGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }

    fun flowEnabledGroups(): Flow<List<String>> {
        return flowEnabledGroupsUnProcessed().map { list ->
            dealGroups(list)
        }.flowOn(IO)
    }
}