package io.legado.app.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import io.legado.app.data.entities.ReplaceRule


@Dao
interface ReplaceRuleDao {

    @Query("SELECT * FROM replace_rules ORDER BY sortOrder ASC")
    fun observeAll(): DataSource.Factory<Int, ReplaceRule>

    @Query("SELECT id FROM replace_rules ORDER BY sortOrder ASC")
    fun observeAllIds(): LiveData<List<Long>>

    @get:Query("SELECT MAX(sortOrder) FROM replace_rules")
    val maxOrder: Int

    @get:Query("SELECT * FROM replace_rules ORDER BY sortOrder ASC")
    val all: List<ReplaceRule>

    @get:Query("SELECT * FROM replace_rules WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    val allEnabled: List<ReplaceRule>

    @Query("SELECT * FROM replace_rules WHERE id = :id")
    fun findById(id: Long): ReplaceRule?

    @Query("SELECT * FROM replace_rules WHERE id in (:ids)")
    fun findByIds(vararg ids: Long): List<ReplaceRule>

    @Query(
        """SELECT * FROM replace_rules WHERE isEnabled = 1 
        AND (scope LIKE '%' || :scope || '%' or scope = null or scope = '')"""
    )
    fun findEnabledByScope(scope: String): List<ReplaceRule>

    @Query(
        """SELECT * FROM replace_rules WHERE isEnabled = 1 
        AND (scope LIKE '%' || :name || '%' or scope LIKE '%' || :origin || '%' or scope = null or scope = '')"""
    )
    fun findEnabledByScope(name: String, origin: String): List<ReplaceRule>

    @Query("select `group` from replace_rules")
    fun observeGroup(): LiveData<List<String>>

    @Query("select * from replace_rules where `group` like '%' || :group || '%'")
    fun getByGroup(group: String): List<ReplaceRule>

    @get:Query("select * from replace_rules where `group` = null or `group` = ''")
    val noGroup: List<ReplaceRule>

    @get:Query("SELECT COUNT(*) - SUM(isEnabled) FROM replace_rules")
    val summary: Int

    @Query("UPDATE replace_rules SET isEnabled = :enable")
    fun enableAll(enable: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg replaceRule: ReplaceRule): List<Long>

    @Update
    fun update(replaceRules: ReplaceRule)

    @Update
    fun update(vararg replaceRules: ReplaceRule)

    @Delete
    fun delete(replaceRules: ReplaceRule)

    @Delete
    fun delete(vararg replaceRules: ReplaceRule)
}