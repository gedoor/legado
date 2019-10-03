package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun liveDataAll(): LiveData<List<BookSource>>

    @Query("select * from book_sources where bookSourceName like :searchKey or bookSourceGroup like :searchKey or bookSourceUrl like :searchKey order by customOrder asc")
    fun liveDataSearch(searchKey: String = ""): LiveData<List<BookSource>>

    @Query("select * from book_sources where enabledExplore = 1 and exploreUrl is not null and exploreUrl <> '' order by customOrder asc")
    fun liveExplore(): LiveData<List<BookSource>>

    @Query("select * from book_sources where enabledExplore = 1 and exploreUrl is not null and exploreUrl <> '' and (bookSourceGroup like :key or bookSourceName like :key) order by customOrder asc")
    fun liveExplore(key: String): LiveData<List<BookSource>>

    @Query("select bookSourceGroup from book_sources where bookSourceGroup is not null and bookSourceGroup <> ''")
    fun liveGroup(): LiveData<List<String>>

    @Query("select distinct  enabled from book_sources where bookSourceName like :searchKey or bookSourceGroup like :searchKey or bookSourceUrl like :searchKey")
    fun searchIsEnable(searchKey: String = ""): List<Boolean>

    @Query("update book_sources set enabled = 1 where bookSourceUrl in (:sourceUrls)")
    fun enableSection(vararg sourceUrls: String)

    @Query("update book_sources set enabled = 0 where bookSourceUrl in (:sourceUrls)")
    fun disableSection(vararg sourceUrls: String)

    @Query("delete from book_sources where bookSourceUrl in (:sourceUrls)")
    fun delSection(vararg sourceUrls: String)

    @Query("select * from book_sources where enabledExplore = 1 order by customOrder asc")
    fun observeFind(): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where bookSourceGroup like '%' || :group || '%'")
    fun getByGroup(group: String): List<BookSource>

    @get:Query("select * from book_sources where bookSourceGroup is null or bookSourceGroup = ''")
    val noGroup: List<BookSource>

    @get:Query("select * from book_sources order by customOrder asc")
    val all: List<BookSource>

    @get:Query("select * from book_sources where enabled = 1 order by customOrder asc")
    val allEnabled: List<BookSource>

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