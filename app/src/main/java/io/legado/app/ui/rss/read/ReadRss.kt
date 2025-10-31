package io.legado.app.ui.rss.read

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssReadRecord
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity

object ReadRss {
    fun readRss(activity: AppCompatActivity, record: RssReadRecord) {
        when (record.type) {
            0 -> {
                activity.startActivity<ReadRssActivity> {
                    putExtra("title", record.title)
                    putExtra("origin", record.origin)
                    putExtra("link", record.record)
                    putExtra("sort", record.sort)
                }
            }
            1 -> readPhoto(activity, record)
        }
    }

    fun readRss(fragment: Fragment, rssArticle: RssArticle,rssSource: RssSource? = null) {
        when (rssArticle.type) {
            0 -> {
                fragment.startActivity<ReadRssActivity> {
                    putExtra("title", rssArticle.title)
                    putExtra("origin", rssArticle.origin)
                    putExtra("link", rssArticle.link)
                    putExtra("sort", rssArticle.sort)
                }
            }
            1 -> readPhoto(fragment, rssArticle, rssSource)
        }
    }

    private fun readPhoto(fragment: Fragment, rssArticle: RssArticle, rssSource: RssSource? = null) {
        rssSource?.let { s ->
            if (s.ruleContent.isNullOrBlank()) {
                fragment.showDialogFragment(PhotoDialog(rssArticle.link))
            }
        }
    }

    private fun readPhoto(activity: AppCompatActivity, record: RssReadRecord) {
        activity.showDialogFragment(PhotoDialog(record.record))
    }


}