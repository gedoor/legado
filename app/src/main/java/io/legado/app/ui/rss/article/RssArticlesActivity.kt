package io.legado.app.ui.rss.article

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.entities.RssArticle
import io.legado.app.lib.theme.ATH
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.getViewModel
import kotlinx.android.synthetic.main.activity_rss_artivles.*
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult

class RssArticlesActivity : VMBaseActivity<RssArticlesViewModel>(R.layout.activity_rss_artivles),
    RssArticlesAdapter.CallBack {

    override val viewModel: RssArticlesViewModel
        get() = getViewModel(RssArticlesViewModel::class.java)

    private val editSource = 12319
    private var adapter: RssArticlesAdapter? = null
    private var rssArticlesData: LiveData<List<RssArticle>>? = null
    private var url: String? = null

    private var durTouchX = -1000000f
    private var durTouchY = -1000000f

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        viewModel.titleLiveData.observe(this, Observer {
            title_bar.title = it
        })
        url = intent.getStringExtra("url")
        url?.let {
            initData(it)
            viewModel.loadContent(it) {
                refresh_progress_bar.isAutoLoading = false
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_articles, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_source -> viewModel.rssSource?.sourceUrl?.let {
                startActivityForResult<RssSourceEditActivity>(editSource, Pair("data", it))
            }
            R.id.menu_clear -> {
                intent.getStringExtra("url")?.let {
                    refresh_progress_bar.isAutoLoading = true
                    viewModel.clear(it) {
                        refresh_progress_bar.isAutoLoading = false
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() {
        ATH.applyEdgeEffectColor(recycler_view)
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                ContextCompat.getDrawable(baseContext, R.drawable.ic_divider)?.let {
                    this.setDrawable(it)
                }
            })
        adapter = RssArticlesAdapter(this, this)
        recycler_view.adapter = adapter
        refresh_progress_bar.isAutoLoading = true
        recycler_view.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        durTouchX = event.x
                        durTouchY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (durTouchX == -1000000f) {
                            durTouchX = event.x
                        }
                        if (durTouchY == -1000000f)
                            durTouchY = event.y

                        val dY = event.y - durTouchY  //>0下拉
                        durTouchY = event.y
                        if (!refresh_progress_bar.isAutoLoading && refresh_progress_bar.getSecondDurProgress() == refresh_progress_bar.secondFinalProgress) {
                            if (recycler_view.adapter!!.itemCount > 0) {
                                if (0 == (recycler_view.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()) {
                                    refresh_progress_bar.setSecondDurProgress((refresh_progress_bar.getSecondDurProgress() + dY / 2).toInt())
                                }
                            } else {
                                refresh_progress_bar.setSecondDurProgress((refresh_progress_bar.getSecondDurProgress() + dY / 2).toInt())
                            }
                            return refresh_progress_bar.getSecondDurProgress() > 0
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!refresh_progress_bar.isAutoLoading && refresh_progress_bar.secondMaxProgress > 0 && refresh_progress_bar.getSecondDurProgress() > 0) {
                            if (refresh_progress_bar.getSecondDurProgress() >= refresh_progress_bar.secondMaxProgress) {
                                refresh_progress_bar.isAutoLoading = true
                                url?.let {
                                    viewModel.loadContent(it) {
                                        refresh_progress_bar.isAutoLoading = false
                                    }
                                }
                            } else {
                                refresh_progress_bar.setSecondDurProgressWithAnim(0)
                            }
                        }
                        durTouchX = -1000000f
                        durTouchY = -1000000f
                    }
                }
                return false
            }
        })
    }

    private fun initData(origin: String) {
        rssArticlesData?.removeObservers(this)
        rssArticlesData = App.db.rssArticleDao().liveByOrigin(origin)
        rssArticlesData?.observe(this, Observer {
            adapter?.setItems(it)
        })
    }

    override fun readRss(rssArticle: RssArticle) {
        viewModel.read(rssArticle)
        startActivity<ReadRssActivity>(
            Pair("origin", rssArticle.origin),
            Pair("title", rssArticle.title)
        )
    }
}