package io.legado.app.data.dao

import androidx.room.*
import io.legado.app.data.entities.TxtTocRule

@Dao
interface TxtTocRuleDao {

    @get:Query("select * from txtTocRules order by serialNumber")
    val all: List<TxtTocRule>

    @get:Query("select * from txtTocRules where enable = 1 order by serialNumber")
    val enabled: List<TxtTocRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rule: TxtTocRule)

    @Delete
    fun delete(vararg rule: TxtTocRule)
}