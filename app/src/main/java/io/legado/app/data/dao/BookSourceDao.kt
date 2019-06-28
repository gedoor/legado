package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.BookSource

@Dao
interface BookSourceDao {

    @Query("select * from book_sources order by customOrder asc")
    fun observeAll(): DataSource.Factory<Int, BookSource>

    @Query("select * from book_sources where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey order by customOrder asc")
    fun observeSearch(searchKey: String = ""): DataSource.Factory<Int, BookSource>

    @Query("select distinct  enabled from book_sources where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey")
    fun searchIsEnable(searchKey: String = ""): List<Boolean>

    @Query("UPDATE book_sources SET enabled = :enable where bookSourceName like :searchKey or `bookSourceGroup` like :searchKey or bookSourceUrl like :searchKey")
    fun enableAllSearch(searchKey: String = "", enable: String = "1")

    @Query("select * from book_sources where enabledExplore = 1 order by customOrder asc")
    fun observeFind(): DataSource.Factory<Int, BookSource>

    @get:Query("select * from book_sources order by customOrder asc")
    val all: List<BookSource>

    @Query("select * from book_sources where bookSourceUrl = :key")
    fun findByKey(key: String): BookSource?

    @Query("select count(*) from book_sources")
    fun allCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookSource: BookSource): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookSource: BookSource)

    @Update
    fun update(bookSource: BookSource)

    @Update
    fun update(vararg bookSource: BookSource)

    @Delete
    fun delete(bookSource: BookSource)

    @Delete
    fun delete(vararg bookSource: BookSource)
}