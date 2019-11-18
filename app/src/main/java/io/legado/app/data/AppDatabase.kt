package io.legado.app.data

import android.content.Context
import android.database.Cursor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.legado.app.data.dao.*
import io.legado.app.data.entities.*
import io.legado.app.help.FileHelp
import io.legado.app.utils.GSON
import java.io.File


@Database(
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class, ReplaceRule::class,
        SearchBook::class, SearchKeyword::class, Cookie::class, RssSource::class, Bookmark::class,
        RssArticle::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private const val DATABASE_NAME = "legado.db"

        private val MIGRATION_1_N: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                backup(database)
            }
        }

        fun createDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
//                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_1_N)
                .build()
        }

        private fun backup(database: SupportSQLiteDatabase) {
            val forms = arrayOf("books")
            forms.forEach { form ->
                database.query("select * from $form").let {
                    val ja = JsonArray()
                    while (it.moveToNext()) {
                        val jo = JsonObject()
                        for (i in 0 until it.columnCount) {
                            if (!it.isNull(i)) {
                                when (it.getType(i)) {
                                    Cursor.FIELD_TYPE_FLOAT ->
                                        jo.addProperty(it.getColumnName(i), it.getFloat(i))
                                    Cursor.FIELD_TYPE_INTEGER ->
                                        jo.addProperty(it.getColumnName(i), it.getInt(i))
                                    else -> jo.addProperty(it.getColumnName(i), it.getString(i))
                                }
                            }
                        }
                        ja.add(jo)
                    }
                    it.close()
                    FileHelp.getFile(FileHelp.getCachePath() + File.separator + "db" + File.separator + form + ".json")
                        .writeText(GSON.toJson(ja))
                }
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
    abstract fun cookieDao(): CookieDao
}