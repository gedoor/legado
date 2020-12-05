package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.SourceSub

@Dao
interface SourceSubDao {

    @get:Query("select * from sourceSubs order by customOrder")
    val all: List<SourceSub>

    @Query("select * from sourceSubs order by customOrder")
    fun observeAll(): LiveData<List<SourceSub>>

    @get:Query("select customOrder from sourceSubs order by customOrder limit 0,1")
    val maxOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg sourceSub: SourceSub)

    @Delete
    fun delete(vararg sourceSub: SourceSub)

    @Update
    fun update(vararg sourceSub: SourceSub)
}