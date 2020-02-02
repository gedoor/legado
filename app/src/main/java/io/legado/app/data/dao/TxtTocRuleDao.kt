package io.legado.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.legado.app.data.entities.TxtTocRule
import retrofit2.http.DELETE

@Dao
interface TxtTocRuleDao {

    @get:Query("select * from txtTocRules order by serialNumber")
    val all: List<TxtTocRule>

    @get:Query("select * from txtTocRules where enable = 1 order by serialNumber")
    val enabled: List<TxtTocRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rule: TxtTocRule)

    @DELETE
    fun delete(vararg rule: TxtTocRule)
}