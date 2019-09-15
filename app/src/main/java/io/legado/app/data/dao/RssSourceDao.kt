package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import io.legado.app.data.entities.RssSource

@Dao
interface RssSourceDao {

    @get:Query("SELECT * FROM rssSources")
    val all: List<RssSource>

    @Query("SELECT * FROM rssSources")
    fun liveAll(): LiveData<List<RssSource>>

    @Query("SELECT * FROM rssSources where enabled = 1")
    fun liveEnabled(): LiveData<List<RssSource>>
}