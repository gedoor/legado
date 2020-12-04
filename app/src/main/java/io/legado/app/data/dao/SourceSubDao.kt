package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.SourceSub

@Dao
interface SourceSubDao {

    @get:Query("select * from sourceSubs")
    val all: List<SourceSub>

    @Query("select * from sourceSubs")
    fun observeAll(): LiveData<List<SourceSub>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg sourceSub: SourceSub)

}