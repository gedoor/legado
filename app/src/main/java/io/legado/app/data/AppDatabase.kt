package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import io.legado.app.help.storage.Restore


@Database(
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        var DestructiveMigration = false
        private const val DATABASE_NAME = "legado.db"
        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        DestructiveMigration = true
                        Restore.restore()
                    }
                })
                .build()
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
    abstract fun cookieDao(): CookieDao
}