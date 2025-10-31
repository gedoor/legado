package io.legado.app.help.gsyVideo

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.DiscontinuityReason
import androidx.media3.common.Player.PositionInfo
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultRenderersFactory.ExtensionRendererMode
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.help.exoplayer.ExoPlayerHelper
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import tv.danmaku.ijk.media.exo2.demo.EventLogger

class Exo2MediaPlayer(context: Context) : IjkExo2MediaPlayer(context) {
    companion object {
        private const val TAG = "GSYExo2MediaPlayer"

        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS: Long = 3000

        const val POSITION_DISCONTINUITY: Int = 899
    }
    private val window = Timeline.Window()


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
        Handler(Looper.myLooper()!!).post {
            if (mTrackSelector == null) {
                mTrackSelector = DefaultTrackSelector(mAppContext)
            }
            mEventLogger = EventLogger(mTrackSelector)
            val preferExtensionDecoders = true
            val useExtensionRenderers = true //是否开启扩展
            val extensionRendererMode: @ExtensionRendererMode Int =
                if (useExtensionRenderers) (if (preferExtensionDecoders) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON) else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            if (mRendererFactory == null) {
                mRendererFactory = DefaultRenderersFactory(mAppContext)
                mRendererFactory.setExtensionRendererMode(extensionRendererMode)
            }
            if (mLoadControl == null) {
                mLoadControl = DefaultLoadControl()
            }
            mInternalPlayer =
                ExoPlayer.Builder(mAppContext, mRendererFactory).setLooper(Looper.myLooper()!!)
                    .setTrackSelector(mTrackSelector).setLoadControl(mLoadControl)
                    .setMediaSourceFactory(
                        DefaultMediaSourceFactory(
                            ResolvingDataSource.Factory(ExoPlayerHelper.cacheDataSourceFactory){ it }
                        )
                            .setLiveTargetOffsetMs(5000)
                    ).build()
            mInternalPlayer.addListener(this@Exo2MediaPlayer)
            mInternalPlayer.addAnalyticsListener(this@Exo2MediaPlayer)
            mInternalPlayer.addListener(mEventLogger)
            if (mSpeedPlaybackParameters != null) {
                mInternalPlayer.playbackParameters = mSpeedPlaybackParameters
            }
            if (isLooping) {
                mInternalPlayer.repeatMode = Player.REPEAT_MODE_ALL
            }
            if (mSurface != null) mInternalPlayer.setVideoSurface(mSurface)
            mInternalPlayer.setMediaSource(mMediaSource)
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
