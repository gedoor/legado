package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun observeAll(): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey order by customOrder asc")
    fun observeSearch(searchKey: String = ""): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where enabledExplore = 1 and exploreUrl is not null and exploreUrl <> '' order by customOrder asc")
    fun liveExplore(): LiveData<List<BookSource>>

    @Query("select bookSourceGroup from book_sources where bookSourceGroup is not null and bookSourceGroup <> ''")
    fun liveGroup(): LiveData<List<String>>

    @Query("select distinct  enabled from book_sources where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey")
    fun searchIsEnable(searchKey: String = ""): List<Boolean>

    @Query("UPDATE book_sources SET enabled = :enable where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey")
    fun enableAllSearch(searchKey: String = "", enable: String = "1")

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

    @get:Query("select min(customOrder) from book_sources")
    val minOrder: Int
}