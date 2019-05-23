package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.BookDao
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.Chapter
import io.legado.app.data.entities.ReplaceRule
import javax.xml.transform.Source


@Database(entities = [Book::class, Chapter::class, ReplaceRule::class, Source::class], version = 1, exportSchema = true)
// @TypeConverters(Converters::class)
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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                })
                .build()
        }

    }

    abstract fun bookDao(): BookDao

}