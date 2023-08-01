package io.legado.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import io.legado.app.help.DefaultData
import org.intellij.lang.annotations.Language
import splitties.init.appCtx
import java.util.*

val appDb by lazy {
    Room.databaseBuilder(appCtx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9)
        .addMigrations(*DatabaseMigrations.migrations)
        .allowMainThreadQueries()
        .addCallback(AppDatabase.dbCallback)
        .build()
}

@Database(
    version = 71,
    exportSchema = true,
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class, ReadRecord::class, HttpTTS::class, Cache::class,
        RuleSub::class, DictRule::class, KeyboardAssist::class, Server::class],
    autoMigrations = [
        AutoMigration(from = 43, to = 44),
        AutoMigration(from = 44, to = 45),
        AutoMigration(from = 45, to = 46),
        AutoMigration(from = 46, to = 47),
        AutoMigration(from = 47, to = 48),
        AutoMigration(from = 48, to = 49),
        AutoMigration(from = 49, to = 50),
        AutoMigration(from = 50, to = 51),
        AutoMigration(from = 51, to = 52),
        AutoMigration(from = 52, to = 53),
        AutoMigration(from = 53, to = 54),
        AutoMigration(from = 54, to = 55, spec = DatabaseMigrations.Migration_54_55::class),
        AutoMigration(from = 55, to = 56),
        AutoMigration(from = 56, to = 57),
        AutoMigration(from = 57, to = 58),
        AutoMigration(from = 58, to = 59),
        AutoMigration(from = 59, to = 60),
        AutoMigration(from = 60, to = 61),
        AutoMigration(from = 61, to = 62),
        AutoMigration(from = 62, to = 63),
        AutoMigration(from = 63, to = 64),
        AutoMigration(from = 64, to = 65, spec = DatabaseMigrations.Migration_64_65::class),
        AutoMigration(from = 65, to = 66),
        AutoMigration(from = 66, to = 67),
        AutoMigration(from = 67, to = 68),
        AutoMigration(from = 68, to = 69),
        AutoMigration(from = 69, to = 70),
        AutoMigration(from = 70, to = 71),
    ]
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
    abstract val dictRuleDao: DictRuleDao
    abstract val keyboardAssistsDao: KeyboardAssistsDao
    abstract val serverDao: ServerDao

    companion object {

        const val DATABASE_NAME = "legado.db"

        val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                db.setLocale(Locale.CHINESE)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                @Language("sql")
                val insertBookGroupAllSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAll}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAll})
                """.trimIndent()
                db.execSQL(insertBookGroupAllSql)
                @Language("sql")
                val insertBookGroupLocalSql = """
                    insert into book_groups(groupId, groupName, 'order', enableRefresh, show) 
                    select ${BookGroup.IdLocal}, '本地', -9, 0, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocal})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalSql)
                @Language("sql")
                val insertBookGroupMusicSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAudio}, '音频', -8, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAudio})
                """.trimIndent()
                db.execSQL(insertBookGroupMusicSql)
                @Language("sql")
                val insertBookGroupNetNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdNetNone}, '网络未分组', -7, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdNetNone})
                """.trimIndent()
                db.execSQL(insertBookGroupNetNoneGroupSql)
                @Language("sql")
                val insertBookGroupLocalNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdLocalNone}, '本地未分组', -6, 0
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocalNone})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalNoneGroupSql)
                @Language("sql")
                val insertBookGroupErrorSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdError}, '更新失败', -1, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdError})
                """.trimIndent()
                db.execSQL(insertBookGroupErrorSql)
                @Language("sql")
                val upBookSourceLoginUiSql =
                    "update book_sources set loginUi = null where loginUi = 'null'"
                db.execSQL(upBookSourceLoginUiSql)
                @Language("sql")
                val upRssSourceLoginUiSql =
                    "update rssSources set loginUi = null where loginUi = 'null'"
                db.execSQL(upRssSourceLoginUiSql)
                @Language("sql")
                val upHttpTtsLoginUiSql =
                    "update httpTTS set loginUi = null where loginUi = 'null'"
                db.execSQL(upHttpTtsLoginUiSql)
                @Language("sql")
                val upHttpTtsConcurrentRateSql =
                    "update httpTTS set concurrentRate = '0' where concurrentRate is null"
                db.execSQL(upHttpTtsConcurrentRateSql)
                db.query("select * from keyboardAssists order by serialNo").use {
                    if (it.count == 0) {
                        DefaultData.keyboardAssists.forEach { keyboardAssist ->
                            val contentValues = ContentValues().apply {
                                put("type", keyboardAssist.type)
                                put("key", keyboardAssist.key)
                                put("value", keyboardAssist.value)
                                put("serialNo", keyboardAssist.serialNo)
                            }
                            db.insert(
                                "keyboardAssists",
                                SQLiteDatabase.CONFLICT_REPLACE,
                                contentValues
                            )
                        }
                    }
                }
            }
        }

    }

}