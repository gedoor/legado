package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.constant.AppConst
import io.legado.app.constant.BookType
import io.legado.app.data.entities.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query(
        """
        select * from books where type != ${BookType.audio} 
        and origin != '${BookType.local}' 
        and ((SELECT sum(groupId) FROM book_groups where groupId > 0) & `group`) = 0
        and (select show from book_groups where groupId = ${AppConst.bookGroupNoneId}) != 1
        """
    )
    fun flowRoot(): Flow<List<Book>>

    @Query("SELECT * FROM books order by durChapterTime desc")
    fun flowAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE type = ${BookType.audio}")
    fun flowAudio(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE origin = '${BookType.local}'")
    fun flowLocal(): Flow<List<Book>>

    @Query(
        """
        select * from books 
        where ((SELECT sum(groupId) FROM book_groups where groupId > 0) & `group`) = 0
        """
    )
    fun flowNoGroup(): Flow<List<Book>>

    @Query("SELECT bookUrl FROM books WHERE origin = '${BookType.local}'")
    fun flowLocalUri(): Flow<List<String>>

    @Query("SELECT * FROM books WHERE (`group` & :group) > 0")
    fun flowByGroup(group: Long): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE name like '%'||:key||'%' or author like '%'||:key||'%'")
    fun flowSearch(key: String): Flow<List<Book>>

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

    @get:Query("select min(`order`) from books")
    val minOrder: Int

    @get:Query("select max(`order`) from books")
    val maxOrder: Int

    @Query("select 1 from books where bookUrl = :bookUrl")
    fun has(bookUrl: String): Boolean?

    @Query("select 1 from books where originName = :fileName")
    fun hasFile(fileName: String): Boolean?

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