package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@Database(
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    companion object {

        private const val DATABASE_NAME = "legado.db"

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .addMigrations(migration_10_11, migration_11_12, migration_12_13)
                .addCallback(object : Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        GlobalScope.launch { Restore.restoreDatabase(Backup.backupPath) }
                    }
                })
                .allowMainThreadQueries()
                .build()
        }

        private val migration_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE txtTocRules")
                database.execSQL(
                    """
                    CREATE TABLE txtTocRules(id INTEGER NOT NULL, 
                    name TEXT NOT NULL, rule TEXT NOT NULL, serialNumber INTEGER NOT NULL, 
                    enable INTEGER NOT NULL, PRIMARY KEY (id))
                    """
                )
            }
        }

        private val migration_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE rssSources ADD style TEXT
                    """
                )
            }
        }

        private val migration_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE rssSources ADD articleStyle INTEGER NOT NULL DEFAULT 0
                    """
                )
            }
        }
    }

    abstract fun bookDao(): BookDao
    abstract fun bookGroupDao(): BookGroupDao
    abstract fun bookSourceDao(): BookSourceDao
    abstract fun bookChapterDao(): BookChapterDao
    abstract fun replaceRuleDao(): ReplaceRuleDao
    abstract fun searchBookDao(): SearchBookDao
    abstract fun searchKeywordDao(): SearchKeywordDao
    abstract fun rssSourceDao(): RssSourceDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun rssArticleDao(): RssArticleDao
    abstract fun rssStarDao(): RssStarDao
    abstract fun cookieDao(): CookieDao
    abstract fun txtTocRule(): TxtTocRuleDao
}