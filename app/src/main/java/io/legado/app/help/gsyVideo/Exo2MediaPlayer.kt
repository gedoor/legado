package io.legado.app.help.gsyVideo

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player.DiscontinuityReason
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.ExtensionRendererMode
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ConcatenatingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.model.webBook.WebBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import tv.danmaku.ijk.media.exo2.demo.EventLogger

class Exo2MediaPlayer(context: Context) : IjkExo2MediaPlayer(context) {
    companion object {
        private const val TAG = "GSYExo2MediaPlayer"

        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS: Long = 3000

        const val POSITION_DISCONTINUITY: Int = 899
    }
    private val window = Timeline.Window()

    private var playIndex = 0
    private var uris = mutableListOf<BookChapter>()
    private var book: Book? = null
    private var source: BookSource? = null
    private var cache = false


    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
        reason: @DiscontinuityReason Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        notifyOnInfo(POSITION_DISCONTINUITY, reason)
    }

    fun setDataSource(
        uris: List<BookChapter>?,
        book: Book?,
        source: BookSource?,
        headers: MutableMap<String?, String?>?,
        index: Int,
        cache: Boolean
    ) {
        mHeaders = headers
        if (uris == null) {
            return
        }
        this.cache = cache
        this.uris.addAll(uris)
        this.book = book
        this.source = source
        playIndex = index
        // 将mMediaSource移到下面prepareAsyncInternal异步时获取


        /** ConcatenatingMediaSource2 是把多个视频拼成一个播放，时间轴只有一个 */
//        val mediaSourceBuilder = ConcatenatingMediaSource2.Builder()
//        for (uri in uris) {
//            val mediaSource: MediaSource = mExoHelper.getMediaSource(
//                uri,
//                isPreview,
//                cache,
//                false,
//                mCacheDir,
//                getOverrideExtension()
//            )
//            mediaSourceBuilder.add(mediaSource,0)
//        }
//        playIndex = index
//        mMediaSource = mediaSourceBuilder.build()
    }


    /**
     * 上一集
     */
    fun previous() {
        if (mInternalPlayer == null) {
            return
        }
        val timeline: Timeline = mInternalPlayer.currentTimeline
        if (timeline.isEmpty) {
            return
        }
        val windowIndex: Int = mInternalPlayer.currentMediaItemIndex
        timeline.getWindow(windowIndex, window)
        val previousWindowIndex: Int = mInternalPlayer.previousMediaItemIndex
        if (previousWindowIndex != C.INDEX_UNSET
            && (mInternalPlayer.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                    || (window.isDynamic && !window.isSeekable))
        ) {
            mInternalPlayer.seekTo(previousWindowIndex, C.TIME_UNSET)
        } else {
            mInternalPlayer.seekTo(0)
        }
    }

    @OptIn(UnstableApi::class)
    override fun prepareAsyncInternal() {
        Handler(Looper.getMainLooper()).post {
            if (mTrackSelector == null) {
                mTrackSelector = DefaultTrackSelector(mAppContext)
            }
            mEventLogger = EventLogger(mTrackSelector) //日志输出,可以删掉
            val preferExtensionDecoders = true
            val useExtensionRenderers = true //是否开启扩展
            val extensionRendererMode: @ExtensionRendererMode Int =
                if (useExtensionRenderers)
                    (if (preferExtensionDecoders)
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    else
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                else
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            if (mRendererFactory == null) {
                mRendererFactory = DefaultRenderersFactory(mAppContext)
                mRendererFactory.setExtensionRendererMode(extensionRendererMode)
            }
            if (mLoadControl == null) {
                mLoadControl = DefaultLoadControl()
            }
            mInternalPlayer = ExoPlayer.Builder(mAppContext, mRendererFactory)
                .setLooper(Looper.getMainLooper())
                .setTrackSelector(mTrackSelector)
                .setLoadControl(mLoadControl).build()

            mInternalPlayer.addListener(this@Exo2MediaPlayer)
            mInternalPlayer.addAnalyticsListener(this@Exo2MediaPlayer)
            mInternalPlayer.addListener(mEventLogger)
            if (mSpeedPlaybackParameters != null) {
                mInternalPlayer.setPlaybackParameters(mSpeedPlaybackParameters)
            }
            if (mSurface != null) mInternalPlayer.setVideoSurface(mSurface)
            /**fix start index */
            /**fix start index */
            if (playIndex > 0) {
                mInternalPlayer.seekTo(playIndex, C.INDEX_UNSET.toLong())
            }
            val concatenatedSource = ConcatenatingMediaSource()
            for (uri in uris) {
                val videoUrl = if (source != null && book != null) {
                    runBlocking(Dispatchers.IO) {
                        WebBook.getContentAwait(source!!, book!!, uri)
                    }
                } else {
                    uri.url
                }
                val mediaSource: MediaSource = mExoHelper.getMediaSource(
                    videoUrl,
                    isPreview,
                    cache,
                    false,
                    mCacheDir,
                    overrideExtension
                )
                concatenatedSource.addMediaSource(mediaSource)
            }
            mMediaSource = concatenatedSource
            mInternalPlayer.setMediaSource(mMediaSource, false)
            mInternalPlayer.prepare()
            mInternalPlayer.playWhenReady = false
        }
    }

    /**
     * 下一集
     */
    fun next() {
        if (mInternalPlayer == null) {
            return
        }
        val timeline: Timeline = mInternalPlayer.currentTimeline
        if (timeline.isEmpty) {
            return
        }
        val windowIndex: Int = mInternalPlayer.currentMediaItemIndex
        val nextWindowIndex: Int = mInternalPlayer.nextMediaItemIndex
        if (nextWindowIndex != C.INDEX_UNSET) {
            mInternalPlayer.seekTo(nextWindowIndex, C.TIME_UNSET)
        } else if (timeline.getWindow(windowIndex, window).isDynamic) {
            mInternalPlayer.seekTo(windowIndex, C.TIME_UNSET)
        }
    }

    val currentWindowIndex: Int
        get() {
            if (mInternalPlayer == null) {
                return 0
            }
            return mInternalPlayer.currentMediaItemIndex
        }


}
