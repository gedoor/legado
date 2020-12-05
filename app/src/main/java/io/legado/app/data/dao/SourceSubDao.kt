package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.SourceSub

@Dao
interface SourceSubDao {

    @get:Query("select * from sourceSubs")
    val all: List<SourceSub>

    @Query("select * from sourceSubs")
    fun observeAll(): LiveData<List<SourceSub>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg sourceSub: SourceSub)

    @Delete
    fun delete(vararg sourceSub: SourceSub)

}