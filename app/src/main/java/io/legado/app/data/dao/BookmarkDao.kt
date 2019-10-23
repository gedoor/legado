package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.Bookmark


@Dao
interface BookmarkDao {

    @Query("select * from bookmarks")
    fun all(): List<Bookmark>

    @Query("select * from bookmarks where bookUrl = :bookUrl")
    fun observeByBook(bookUrl: String): DataSource.Factory<Int, Bookmark>

}