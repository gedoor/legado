package io.legado.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.legado.app.constant.AppConst
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import splitties.init.appCtx
import java.util.*

val appDb by lazy {
    AppDatabase.createDatabase(appCtx)
}

@Database(
    version = 43,
    exportSchema = true,
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class, ReadRecord::class, HttpTTS::class, Cache::class,
        RuleSub::class]
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
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9)
                .addMigrations(*DatabaseMigrations.migrations)
                .allowMainThreadQueries()
                .addCallback(dbCallback)
                .build()

        private val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                db.setLocale(Locale.CHINESE)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) 
                    select ${AppConst.bookGroupAllId}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupAllId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) 
                    select ${AppConst.bookGroupLocalId}, '本地', -9, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupLocalId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) 
                    select ${AppConst.bookGroupAudioId}, '音频', -8, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupAudioId})"""
                )
                db.execSQL(
                    """insert into book_groups(groupId, groupName, 'order', show) 
                    select ${AppConst.bookGroupNoneId}, '未分组', -7, 1
                    where not exists (select * from book_groups where groupId = ${AppConst.bookGroupNoneId})"""
                )
                db.execSQL("update book_sources set loginUi = null where loginUi = 'null'")
                db.execSQL("update rssSources set loginUi = null where loginUi = 'null'")
                db.execSQL("update httpTTS set loginUi = null where loginUi = 'null'")
                db.execSQL("update httpTTS set concurrentRate = '0' where loginUi is null")
            }
        }

    }

}