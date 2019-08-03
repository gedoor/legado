package io.legado.app.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.RssSource

@Dao
interface RssSourceDao {

    @Query("SELECT * FROM rssSources")
    fun observeAll(): DataSource.Factory<Int, RssSource>

    @Query("SELECT * FROM rssSources where enabled = 1")
    fun observeEnabled(): DataSource.Factory<Int, RssSource>
}