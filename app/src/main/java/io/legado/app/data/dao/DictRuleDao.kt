package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import io.legado.app.data.entities.DictRule

@Dao
interface DictRuleDao {

    @get:Query("select * from dictRules")
    val all: List<DictRule>

    @get:Query("select * from dictRules where enabled = 1")
    val enabled: List<DictRule>

    @Upsert
    fun upsert(vararg dictRule: DictRule)

    @Delete
    fun delete(vararg dictRule: DictRule)

}