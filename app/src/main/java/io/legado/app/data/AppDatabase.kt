package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppConst.androidId
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import splitties.init.appCtx
import java.util.*

val appDb by lazy {
    AppDatabase.createDatabase(appCtx)
}

@Database(
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class, ReadRecord::class, HttpTTS::class, Cache::class,
        RuleSub::class],
    version = 32,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract val bookDao: BookDao
    abstract val bookGroupDao: BookGroupDao
    abstract val bookSourceDao: BookSourceDao
    abstract val bookChapterDao: BookChapterDao
    abstract val replaceRuleDao: ReplaceRuleDao
    abstract val searchBookDao: SearchBookDao
    abstract val searchKeywordDao: SearchKeywordDao
    abstract val rssSourceDao: RssSourceDao
    abstract val bookmarkDao: BookmarkDao
    abstract val rssArticleDao: RssArticleDao
    abstract val rssStarDao: RssStarDao
    abstract val cookieDao: CookieDao
    abstract val txtTocRuleDao: TxtTocRuleDao
    abstract val readRecordDao: ReadRecordDao
    abstract val httpTTSDao: HttpTTSDao
    abstract val cacheDao: CacheDao
    abstract val ruleSubDao: RuleSubDao

    companion object {

        private const val DATABASE_NAME = "legado.db"

        fun createDatabase(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .addMigrations(
                    migration_10_11, migration_11_12, migration_12_13, migration_13_14,
                    migration_14_15, migration_15_17, migration_17_18, migration_18_19,
                    migration_19_20, migration_20_21, migration_21_22, migration_22_23,
                    migration_23_24, migration_24_25, migration_25_26, migration_26_27,
                    migration_27_28, migration_28_29, migration_29_30, migration_30_31,
                    migration_31_32
                )
                .allowMainThreadQueries()
                .addCallback(dbCallback)
                .build()

        private val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                db.setLocale(Locale.CHINESE)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) select ${AppConst.bookGroupAllId}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupAllId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) select ${AppConst.bookGroupLocalId}, '本地', -9, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupLocalId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) select ${AppConst.bookGroupAudioId}, '音频', -8, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupAudioId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) select ${AppConst.bookGroupNoneId}, '未分组', -7, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupNoneId})"""
                )
            }
        }

        private val migration_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE txtTocRules")
                database.execSQL(
                    """CREATE TABLE txtTocRules(id INTEGER NOT NULL, 
                    name TEXT NOT NULL, rule TEXT NOT NULL, serialNumber INTEGER NOT NULL, 
                    enable INTEGER NOT NULL, PRIMARY KEY (id))"""
                )
            }
        }

        private val migration_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rssSources ADD style TEXT ")
            }
        }

        private val migration_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rssSources ADD articleStyle INTEGER NOT NULL DEFAULT 0 ")
            }
        }

        private val migration_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `books_new` (`bookUrl` TEXT NOT NULL, `tocUrl` TEXT NOT NULL, `origin` TEXT NOT NULL,
                    `originName` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT NOT NULL, `kind` TEXT, `customTag` TEXT, `coverUrl` TEXT, 
                    `customCoverUrl` TEXT, `intro` TEXT, `customIntro` TEXT, `charset` TEXT, `type` INTEGER NOT NULL, `group` INTEGER NOT NULL, 
                    `latestChapterTitle` TEXT, `latestChapterTime` INTEGER NOT NULL, `lastCheckTime` INTEGER NOT NULL, `lastCheckCount` INTEGER NOT NULL, 
                    `totalChapterNum` INTEGER NOT NULL, `durChapterTitle` TEXT, `durChapterIndex` INTEGER NOT NULL, `durChapterPos` INTEGER NOT NULL, 
                    `durChapterTime` INTEGER NOT NULL, `wordCount` TEXT, `canUpdate` INTEGER NOT NULL, `order` INTEGER NOT NULL, 
                    `originOrder` INTEGER NOT NULL, `useReplaceRule` INTEGER NOT NULL, `variable` TEXT, PRIMARY KEY(`bookUrl`))"""
                )
                database.execSQL("INSERT INTO books_new select * from books ")
                database.execSQL("DROP TABLE books")
                database.execSQL("ALTER TABLE books_new RENAME TO books")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_name_author` ON `books` (`name`, `author`) ")
            }
        }

        private val migration_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE bookmarks ADD bookAuthor TEXT NOT NULL DEFAULT ''")
            }
        }

        private val migration_15_17 = object : Migration(15, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `readRecord` (`bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, PRIMARY KEY(`bookName`))")
            }
        }

        private val migration_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `httpTTS` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val migration_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `readRecordNew` (`androidId` TEXT NOT NULL, `bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, 
                    PRIMARY KEY(`androidId`, `bookName`))"""
                )
                database.execSQL("INSERT INTO readRecordNew(androidId, bookName, readTime) select '${androidId}' as androidId, bookName, readTime from readRecord")
                database.execSQL("DROP TABLE readRecord")
                database.execSQL("ALTER TABLE readRecordNew RENAME TO readRecord")
            }
        }
        private val migration_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_sources ADD bookSourceComment TEXT")
            }
        }

        private val migration_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE book_groups ADD show INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val migration_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `books_new` (`bookUrl` TEXT NOT NULL, `tocUrl` TEXT NOT NULL, `origin` TEXT NOT NULL, 
                    `originName` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT NOT NULL, `kind` TEXT, `customTag` TEXT, 
                    `coverUrl` TEXT, `customCoverUrl` TEXT, `intro` TEXT, `customIntro` TEXT, `charset` TEXT, `type` INTEGER NOT NULL, 
                    `group` INTEGER NOT NULL, `latestChapterTitle` TEXT, `latestChapterTime` INTEGER NOT NULL, `lastCheckTime` INTEGER NOT NULL, 
                    `lastCheckCount` INTEGER NOT NULL, `totalChapterNum` INTEGER NOT NULL, `durChapterTitle` TEXT, `durChapterIndex` INTEGER NOT NULL, 
                    `durChapterPos` INTEGER NOT NULL, `durChapterTime` INTEGER NOT NULL, `wordCount` TEXT, `canUpdate` INTEGER NOT NULL, 
                    `order` INTEGER NOT NULL, `originOrder` INTEGER NOT NULL, `variable` TEXT, `readConfig` TEXT, PRIMARY KEY(`bookUrl`))"""
                )
                database.execSQL(
                    """INSERT INTO books_new select `bookUrl`, `tocUrl`, `origin`, `originName`, `name`, `author`, `kind`, `customTag`, `coverUrl`, 
                    `customCoverUrl`, `intro`, `customIntro`, `charset`, `type`, `group`, `latestChapterTitle`, `latestChapterTime`, `lastCheckTime`, 
                    `lastCheckCount`, `totalChapterNum`, `durChapterTitle`, `durChapterIndex`, `durChapterPos`, `durChapterTime`, `wordCount`, `canUpdate`, 
                    `order`, `originOrder`, `variable`, null
                    from books"""
                )
                database.execSQL("DROP TABLE books")
                database.execSQL("ALTER TABLE books_new RENAME TO books")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_books_name_author` ON `books` (`name`, `author`) ")
            }
        }

        private val migration_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapters ADD baseUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        private val migration_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `caches` (`key` TEXT NOT NULL, `value` TEXT, `deadline` INTEGER NOT NULL, PRIMARY KEY(`key`))")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_caches_key` ON `caches` (`key`)")
            }
        }

        private val migration_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `sourceSubs` 
                    (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `type` INTEGER NOT NULL, `customOrder` INTEGER NOT NULL, 
                    PRIMARY KEY(`id`))"""
                )
            }
        }

        private val migration_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `ruleSubs` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `type` INTEGER NOT NULL, 
                    `customOrder` INTEGER NOT NULL, `autoUpdate` INTEGER NOT NULL, `update` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
                )
                database.execSQL(" insert into `ruleSubs` select *, 0, 0 from `sourceSubs` ")
                database.execSQL("DROP TABLE `sourceSubs`")
            }
        }

        private val migration_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(" ALTER TABLE rssSources ADD singleUrl INTEGER NOT NULL DEFAULT 0 ")
                database.execSQL(
                    """CREATE TABLE IF NOT EXISTS `bookmarks1` (`time` INTEGER NOT NULL, `bookUrl` TEXT NOT NULL, `bookName` TEXT NOT NULL, 
                        `bookAuthor` TEXT NOT NULL, `chapterIndex` INTEGER NOT NULL, `chapterPos` INTEGER NOT NULL, `chapterName` TEXT NOT NULL, 
                        `bookText` TEXT NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`time`))"""
                )
                database.execSQL(
                    """insert into `bookmarks1` 
                        select `time`, `bookUrl`, `bookName`, `bookAuthor`, `chapterIndex`, `pageIndex`, `chapterName`, '', `content` 
                        from bookmarks"""
                )
                database.execSQL(" DROP TABLE `bookmarks` ")
                database.execSQL(" ALTER TABLE bookmarks1 RENAME TO bookmarks ")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_time` ON `bookmarks` (`time`)")
            }
        }

        private val migration_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rssArticles ADD variable TEXT")
                database.execSQL("ALTER TABLE rssStars ADD variable TEXT")
            }
        }

        private val migration_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE rssSources ADD sourceComment TEXT")
            }
        }

        private val migration_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapters ADD `startFragmentId` TEXT")
                database.execSQL("ALTER TABLE chapters ADD `endFragmentId` TEXT")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `epubChapters` 
                    (`bookUrl` TEXT NOT NULL, `href` TEXT NOT NULL, `parentHref` TEXT, 
                    PRIMARY KEY(`bookUrl`, `href`), FOREIGN KEY(`bookUrl`) REFERENCES `books`(`bookUrl`) ON UPDATE NO ACTION ON DELETE CASCADE )
                """
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_epubChapters_bookUrl` ON `epubChapters` (`bookUrl`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_epubChapters_bookUrl_href` ON `epubChapters` (`bookUrl`, `href`)")
            }
        }

        private val migration_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE readRecord RENAME TO readRecord1")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `readRecord` (`deviceId` TEXT NOT NULL, `bookName` TEXT NOT NULL, `readTime` INTEGER NOT NULL, PRIMARY KEY(`deviceId`, `bookName`))
                """
                )
                database.execSQL("insert into readRecord (deviceId, bookName, readTime) select androidId, bookName, readTime from readRecord1")
            }
        }

        private val migration_31_32 = object : Migration(31, 32) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE `epubChapters`")
            }
        }
    }

}