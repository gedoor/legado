package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM books order by durChapterTime desc")
    fun observeAll(): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE type = ${BookType.audio}")
    fun observeAudio(): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE origin = '${BookType.local}'")
    fun observeLocal(): LiveData<List<Book>>

    @Query("select * from books where type != ${BookType.audio} and origin != '${BookType.local}' and ((SELECT sum(groupId) FROM book_groups where groupId > 0) & `group`) = 0")
    fun observeNoGroup(): LiveData<List<Book>>

    @Query("SELECT bookUrl FROM books WHERE origin = '${BookType.local}'")
    fun observeLocalUri(): LiveData<List<String>>

    @Query("SELECT * FROM books WHERE (`group` & :group) > 0")
    fun observeByGroup(group: Long): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE name like '%'||:key||'%' or author like '%'||:key||'%'")
    fun liveDataSearch(key: String): LiveData<List<Book>>

    @Query("SELECT * FROM books WHERE (`group` & :group) > 0")
    fun getBooksByGroup(group: Long): List<Book>

    @Query("SELECT * FROM books WHERE `name` in (:names)")
    fun findByName(vararg names: String): List<Book>

    @Query("SELECT * FROM books WHERE bookUrl = :bookUrl")
    fun getBook(bookUrl: String): Book?

    @Query("SELECT * FROM books WHERE name = :name and author = :author")
    fun getBook(name: String, author: String): Book?

    @get:Query("select count(bookUrl) from books where (SELECT sum(groupId) FROM book_groups) & `group` = 0")
    val noGroupSize: Int

    @get:Query("SELECT * FROM books where origin <> '${BookType.local}' and type = 0")
    val webBooks: List<Book>

    @get:Query("SELECT * FROM books where origin <> '${BookType.local}' and canUpdate = 1")
    val hasUpdateBooks: List<Book>

    @get:Query("SELECT * FROM books")
    val all: List<Book>

    @get:Query("SELECT * FROM books where type = 0 ORDER BY durChapterTime DESC limit 1")
    val lastReadBook: Book?

    @get:Query("SELECT bookUrl FROM books")
    val allBookUrls: List<String>

    @get:Query("SELECT COUNT(*) FROM books")
    val allBookCount: Int

    @get:Query("select max(`order`) from books")
    val maxOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg book: Book)

    @Update
    fun update(vararg book: Book)

    @Delete
    fun delete(vararg book: Book)

    @Query("update books set durChapterPos = :pos where bookUrl = :bookUrl")
    fun upProgress(bookUrl: String, pos: Int)

    @Query("update books set `group` = :newGroupId where `group` = :oldGroupId")
    fun upGroup(oldGroupId: Long, newGroupId: Long)

}