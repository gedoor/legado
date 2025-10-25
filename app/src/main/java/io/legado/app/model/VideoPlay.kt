package io.legado.app.model

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Window
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import io.legado.app.constant.AppLog
import io.legado.app.constant.SourceType
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.help.book.update
import io.legado.app.help.gsyVideo.ExoVideoManager
import io.legado.app.help.gsyVideo.ExoVideoManager.Companion.FULLSCREEN_ID
import io.legado.app.help.gsyVideo.FloatingPlayer
import io.legado.app.help.gsyVideo.VideoPlayer
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.externalCache
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import splitties.init.appCtx
import java.io.File

object VideoPlay : CoroutineScope by MainScope(){
    val videoManager by lazy { ExoVideoManager() }
    var videoUrl: String? = null
    var videoTitle: String? = null
    var source: BaseSource? = null
    var book: Book? = null
    var toc: List<BookChapter>? =  null
    var durChapterIndex = 0
    var durChapterPos = 0
    var inBookshelf = true

    /**
     * 开始播放
     */
    fun startPlay(player: StandardGSYVideoPlayer) {
        val player = player.getCurrentPlayer()
        durChapterPos.takeIf { it > 0 }?.toLong()?.let { player.seekOnStart = it }
        if (videoUrl != null) {
            inBookshelf = true
            val analyzeUrl = AnalyzeUrl(
                videoUrl!!,
                source = source,
                ruleData = book,
                chapter = null
            )
//            player.mapHeadData = analyzeUrl.headerMap
            player.setUp(analyzeUrl.url, false, File(appCtx.externalCache, "exoplayer"),analyzeUrl.headerMap.toMap(), videoTitle)
            player.startPlayLogic()
            return
        }
        if (book == null) {
            appCtx.toastOnUi("未找到书籍")
            return
        }
        val chapter = toc?.getOrNull(durChapterIndex)
        if (chapter == null) {
            appCtx.toastOnUi("未找到章节")
            return
        }
        WebBook.getContent(this, source as BookSource, book!!, chapter)
            .onSuccess { content ->
                if (content.isEmpty()) {
                    appCtx.toastOnUi("未获取到资源链接")
                } else {
                    val analyzeUrl = AnalyzeUrl(
                        content,
                        source = source,
                        ruleData = book,
                        chapter = chapter
                    )
                    player.mapHeadData = analyzeUrl.headerMap
                    player.setUp(analyzeUrl.url, false, File(appCtx.externalCache, "exoplayer"), videoTitle)
                    player.startPlayLogic()
                }
            }.onError {
                AppLog.put("获取资源链接出错\n$it", it, true)
            }
    }

    /**
     * 退出全屏，主要用于返回键
     *
     * @return 返回是否全屏
     */
    fun backFromWindowFull(context: Context?): Boolean {
        var backFrom = false
        val vp =
            (CommonUtil.scanForActivity(context)).findViewById<View?>(Window.ID_ANDROID_CONTENT) as ViewGroup
        val oldF = vp.findViewById<View?>(FULLSCREEN_ID)
        if (oldF != null) {
            backFrom = true
            CommonUtil.hideNavKey(context)
            if (videoManager.lastListener() != null) {
                videoManager.lastListener().onBackFullscreen()
            }
        }
        return backFrom
    }
    /**
     * 页面销毁了记得调用是否所有的video
     */
    fun releaseAllVideos() {
        if (videoManager.listener() != null) {
            videoManager.listener().onCompletion()
        }
        videoManager.releaseMediaPlayer()
    }
    /**
     * 暂停播放
     */
    fun onPause() {
        if (videoManager.listener() != null) {
            videoManager.listener().onVideoPause()
        }
    }

    /**
     * 恢复播放
     */
    fun onResume() {
        if (videoManager.listener() != null) {
            videoManager.listener().onVideoResume()
        }
    }


    /**
     * 恢复暂停状态
     *
     * @param seek 是否产生seek动作,直播设置为false
     */
    fun onResume(seek: Boolean) {
        if (videoManager.listener() != null) {
            videoManager.listener().onVideoResume(seek)
        }
    }

    /**
     * 播放器移植 - 辅助函数
     *
     *
     */
    @SuppressLint("StaticFieldLeak")
    private var sSwitchVideo: StandardGSYVideoPlayer? = null
    private var sMediaPlayerListener: GSYMediaPlayerListener? = null
    fun savePlayState(switchVideo: StandardGSYVideoPlayer) {
        when (switchVideo) {
            is VideoPlayer -> sSwitchVideo = switchVideo.saveState()
            is FloatingPlayer -> sSwitchVideo = switchVideo.saveState()
        }
        sMediaPlayerListener = switchVideo
    }

    fun clonePlayState(switchVideo: StandardGSYVideoPlayer) {
        when (switchVideo) {
            is VideoPlayer -> sSwitchVideo?.let { switchVideo.cloneState(it) }
            is FloatingPlayer -> sSwitchVideo?.let { switchVideo.cloneState(it) }
        }
    }

    fun release() {
        sMediaPlayerListener?.onAutoCompletion()
        sSwitchVideo = null
        sMediaPlayerListener = null
    }

    fun initSource(sourceKey: String?, sourceType: Int?, bookUrl: String?): BaseSource? {
        source = sourceKey?.let {
            when (sourceType) {
                SourceType.book -> appDb.bookSourceDao.getBookSource(it)
                SourceType.rss -> appDb.rssSourceDao.getByKey(it)
                else -> null
            }
        }
        book = bookUrl?.let {
            toc = appDb.bookChapterDao.getChapterList(it)
            appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
        }?.also { b ->
            durChapterIndex = b.durChapterIndex
            durChapterPos = b.durChapterPos
            source = appDb.bookSourceDao.getBookSource(b.origin)
        }
        return source
    }
    fun upSource() {
        when (source) {
            is BookSource -> {
                source = appDb.bookSourceDao.getBookSource(source!!.getKey())
            }
            is RssSource -> {
                source = appDb.rssSourceDao.getByKey(source!!.getKey())
            }
        }
    }

    fun upDurIndex(offset: Int): Boolean {
        toc ?: return false
        val index = durChapterIndex + offset
        if (index < 0 || index >= toc!!.size) {
            appCtx.toastOnUi("没有更多章节了")
            return false
        }
        durChapterIndex = index
        durChapterPos = 0
        return true
    }

    fun saveRead() {
        book?.let { book ->
            book.lastCheckCount = 0
            book.durChapterTime = System.currentTimeMillis()
            book.durChapterIndex = durChapterIndex
            book.durChapterPos = durChapterPos
            videoTitle = toc?.getOrNull(durChapterIndex)?.title
            book.durChapterTitle = videoTitle
            book.update()
        }
    }
}