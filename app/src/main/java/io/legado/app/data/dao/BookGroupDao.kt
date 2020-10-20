package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.constant.BookType
import io.legado.app.data.entities.BookGroup

@Dao
interface BookGroupDao {

    @Query("select * from book_groups where groupId = :id")
    fun getByID(id: Long): BookGroup?

    @Query("select * from book_groups where groupName = :groupName")
    fun getByName(groupName: String): BookGroup?

    @Query("SELECT * FROM book_groups ORDER BY `order`")
    fun liveDataAll(): LiveData<List<BookGroup>>

    @Query(
        """
        SELECT * FROM book_groups where groupId >= 0 
        or (groupId = -4 and show > 0 and (select count(bookUrl) from books where type != ${BookType.audio} and origin != '${BookType.local}' and ((SELECT sum(groupId) FROM book_groups where groupId > 0) & `group`) = 0) > 0)
        or (groupId = -3 and show > 0 and (select count(bookUrl) from books where type = ${BookType.audio}) > 0)
        or (groupId = -2 and show > 0 and (select count(bookUrl) from books where origin = '${BookType.local}') > 0)
        or (groupId = -1 and show > 0)
        ORDER BY `order`
    """
    )
    fun liveDataShow(): LiveData<List<BookGroup>>

    @Query("SELECT * FROM book_groups where groupId >= 0 ORDER BY `order`")
    fun liveDataSelect(): LiveData<List<BookGroup>>

    @get:Query("SELECT sum(groupId) FROM book_groups where groupId >= 0")
    val idsSum: Long

    @get:Query("SELECT MAX(`order`) FROM book_groups where groupId >= 0")
    val maxOrder: Int

    @get:Query("SELECT * FROM book_groups ORDER BY `order`")
    val all: List<BookGroup>

    @Query("update book_groups set show = 1 where groupId = :groupId")
    fun enableGroup(groupId: Long)

    @Query("select groupName from book_groups where groupId > 0 and (groupId & :id) > 0")
    fun getGroupNames(id: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookGroup: BookGroup)

    @Update
    fun update(vararg bookGroup: BookGroup)

    @Delete
    fun delete(vararg bookGroup: BookGroup)
}