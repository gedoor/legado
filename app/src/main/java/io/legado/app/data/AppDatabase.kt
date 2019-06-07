package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.BookDao
import io.legado.app.data.dao.ReplaceRuleDao
import io.legado.app.data.dao.BookSourceDao
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.Chapter
import io.legado.app.data.entities.ReplaceRule


@Database(entities = [Book::class, BookSource::class, Chapter::class, ReplaceRule::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private const val DATABASE_NAME = "legado.db"

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.run {
                    // execSQL("ALTER TABLE parsers ADD COLUMN fulltextScript TEXT")
                    // execSQL("ALTER TABLE feeds ADD COLUMN lastUpdateTime INTEGER NOT NULL DEFAULT 0")
                    // execSQL("DELETE FROM entries WHERE rowid NOT IN (SELECT MIN(rowid) FROM entries GROUP BY link)")
                    // execSQL("CREATE UNIQUE INDEX index_entries_link ON entries(link)")
                }
            }
        }

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                // .addMigrations(MIGRATION_1_2)
                // .addMigrations(MIGRATION_2_3)
                // .addMigrations(MIGRATION_3_4)
                // .addMigrations(MIGRATION_4_5)
                // .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
        }

    }

    abstract fun bookDao(): BookDao
    abstract fun replaceRuleDao(): ReplaceRuleDao
    abstract fun bookSourceDao() : BookSourceDao
}