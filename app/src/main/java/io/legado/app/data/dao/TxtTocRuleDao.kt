package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import io.legado.app.data.entities.TxtTocRule

@Dao
interface TxtTocRuleDao {

    @Query("select * from txtTocRules order by serialNumber")
    fun observeAll(): LiveData<List<TxtTocRule>>

    @get:Query("select * from txtTocRules order by serialNumber")
    val all: List<TxtTocRule>

    @get:Query("select * from txtTocRules where enable = 1 order by serialNumber")
    val enabled: List<TxtTocRule>

    @get:Query("select ifNull(max(serialNumber), 0) from txtTocRules")
    val lastOrderNum: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rule: TxtTocRule)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg rule: TxtTocRule)

    @Delete
    fun delete(vararg rule: TxtTocRule)

    @Query("delete from txtTocRules where id < 0")
    fun deleteDefault()
}