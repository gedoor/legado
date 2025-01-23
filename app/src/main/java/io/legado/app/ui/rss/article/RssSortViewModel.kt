package io.legado.app.ui.rss.article

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.help.source.removeSortCache


class RssSortViewModel(application: Application) : BaseViewModel(application) {
    var url: String? = null
    var rssSource: RssSource? = null
    val titleLiveData = MutableLiveData<String>()
    var order = System.currentTimeMillis()
    val isGridLayout get() = rssSource?.articleStyle == 2

    fun initData(intent: Intent, finally: () -> Unit) {
        execute {
            url = intent.getStringExtra("url")
            url?.let { url ->
                rssSource = appDb.rssSourceDao.getByKey(url)
                rssSource?.let {
                    titleLiveData.postValue(it.sourceName)
                } ?: let {
                    rssSource = RssSource(sourceUrl = url)
                }
            }
        }.onFinally {
            finally()
        }
    }

    fun switchLayout() {
        rssSource?.let {
            if (it.articleStyle < 2) {
                it.articleStyle += 1
            } else {
                it.articleStyle = 0
            }
            execute {
                appDb.rssSourceDao.update(it)
            }
        }
    }

    fun read(rssArticle: RssArticle) {
        execute {
            val rssReadRecord = RssReadRecord(
                record = rssArticle.link,
                title = rssArticle.title,
                readTime = System.currentTimeMillis()
            )
            appDb.rssReadRecordDao.insertRecord(rssReadRecord)
        }
    }

    fun clearArticles() {
        execute {
            url?.let {
                appDb.rssArticleDao.delete(it)
            }
            order = System.currentTimeMillis()
        }.onSuccess {

        }
    }

    fun clearSortCache(onFinally: () -> Unit) {
        execute {
            rssSource?.removeSortCache()
        }.onFinally {
            onFinally.invoke()
        }
    }

    fun getRecord(): List<RssReadRecord> {
        return appDb.rssReadRecordDao.getRecord()
    }

    fun countRead() : Int {
        return appDb.rssReadRecordDao.countRead
    }

    fun delReadRecord() {
        execute {
            appDb.rssReadRecordDao.deleteRecord()
        }
    }

}