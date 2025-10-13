package io.legado.app.help.gsyVideo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.shuyu.gsyvideoplayer.GSYVideoBaseManager
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener
import com.shuyu.gsyvideoplayer.utils.CommonUtil
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import io.legado.app.R
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import java.io.File

/**
基类管理器
 */
class ExoVideoManager: GSYVideoBaseManager() {
    companion object {
        val SMALL_ID: Int = R.id.small_id
        val FULLSCREEN_ID: Int = R.id.full_id
        var TAG: String = "GSYExoVideoManager"
        @SuppressLint("StaticFieldLeak")
        var videoManager: ExoVideoManager? = null
        /**
         * 单例管理器
         */
        @Synchronized
        fun instance(): ExoVideoManager {
            return videoManager ?: run {
                val newInstance = ExoVideoManager()
                videoManager = newInstance
                newInstance
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
                if (instance()
                        .lastListener() != null
                ) {
                    instance().lastListener()
                        .onBackFullscreen()
                }
            }
            return backFrom
        }

        /**
         * 页面销毁了记得调用是否所有的video
         */
        fun releaseAllVideos() {
            if (instance().listener() != null) {
                instance().listener().onCompletion()
            }
            instance().releaseMediaPlayer()
        }


        /**
         * 暂停播放
         */
        fun onPause() {
            if (instance().listener() != null) {
                instance().listener().onVideoPause()
            }
        }

        /**
         * 恢复播放
         */
        fun onResume() {
            if (instance().listener() != null) {
                instance().listener().onVideoResume()
            }
        }


        /**
         * 恢复暂停状态
         *
         * @param seek 是否产生seek动作,直播设置为false
         */
        fun onResume(seek: Boolean) {
            if (instance().listener() != null) {
                instance().listener()
                    .onVideoResume(seek)
            }
        }

        /**
         * 当前是否全屏状态
         *
         * @return 当前是否全屏状态， true代表是。
         */
        fun isFullState(activity: Activity?): Boolean {
            val vp =
                (CommonUtil.scanForActivity(activity)).findViewById<View?>(Window.ID_ANDROID_CONTENT) as ViewGroup
            val full = vp.findViewById<View?>(FULLSCREEN_ID)
            var gsyVideoPlayer: GSYVideoPlayer? = null
            if (full != null) {
                gsyVideoPlayer = full as GSYVideoPlayer
            }
            return gsyVideoPlayer != null
        }

        /**
         * 持有播放目录列表 - 播放列表
         *
         *
         */
        var mUrlList: List<BookChapter>? =  null
        var mBook: Book? = null
        var mSource: BaseSource? = null
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


    }

    init {
        init()
    }

    @OptIn(UnstableApi::class)
    override fun getPlayManager(): ExoPlayerManager {
        return ExoPlayerManager()
    }

    fun prepare(
        urls: List<BookChapter>,
        book: Book,
        source: BookSource,
        mapHeadData: MutableMap<String?, String?>?,
        index: Int,
        loop: Boolean,
        speed: Float,
        cache: Boolean,
        cachePath: File?,
        overrideExtension: String?
    ) {
        if (urls.isEmpty()) return
        val msg = Message()
        msg.what = HANDLER_PREPARE
        msg.obj =
            GSYExoModel(urls, book, source, mapHeadData, index, loop, speed, cache, cachePath, overrideExtension)
        sendMessage(msg)
    }
    /**
     * 上一集
     */
    @OptIn(UnstableApi::class)
    fun previous() {
        if (playerManager == null) {
            return
        }
        (playerManager as ExoPlayerManager).previous()
    }


    fun setDisplayNew(holder: Any?) {
        val msg = Message()
        msg.what = HANDLER_SETDISPLAY
        msg.obj = holder
        if (playerManager != null) {
            playerManager.showDisplay(msg)
        }
    }

    /**
     * 下一集
     */
    @OptIn(UnstableApi::class)
    fun next() {
        if (playerManager == null) {
            return
        }
        (playerManager as ExoPlayerManager).next()
    }

}