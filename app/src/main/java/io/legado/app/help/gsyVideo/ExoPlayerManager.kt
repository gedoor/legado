package io.legado.app.help.gsyVideo

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Message
import android.view.Surface
import android.view.SurfaceControl
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.video.PlaceholderSurface
import com.shuyu.gsyvideoplayer.cache.ICacheManager
import com.shuyu.gsyvideoplayer.model.GSYModel
import com.shuyu.gsyvideoplayer.model.VideoOptionModel
import com.shuyu.gsyvideoplayer.player.BasePlayerManager
import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer


/**
 * 播放器差异管理接口
 */
@UnstableApi
class ExoPlayerManager : BasePlayerManager() {
    companion object {
        private const val SURFACE_CONTROL_NAME = "surfacedemo"
    }

    private var surface: Surface? = null

    private var dummySurface: PlaceholderSurface? = null
    private var surfaceControl: SurfaceControl? = null
    private var videoSurface: Surface? = null
    private var mediaPlayer: Exo2MediaPlayer? = null
    override fun getMediaPlayer(): IMediaPlayer? {
        return mediaPlayer
    }

    override fun initVideoPlayer(
        context: Context,
        msg: Message,
        optionModelList: List<VideoOptionModel>?,
        cacheManager: ICacheManager
    ) {
        mediaPlayer = Exo2MediaPlayer(context)
        mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (dummySurface == null) {
            dummySurface = PlaceholderSurface.newInstanceV17(context, false)
        }
        val model = msg.obj as GSYModel
        try {
            if (model.url.isNullOrEmpty()) {
                // 处理URL为空的情况
                return
            }
            mediaPlayer!!.setLooping(model.isLooping)
            mediaPlayer!!.setPreview(model.getMapHeadData() != null && model.getMapHeadData().isNotEmpty())
            if (model.isCache()) {
                //通过管理器处理
                cacheManager.doCacheLogic(
                    context,
                    mediaPlayer,
                    model.getUrl(),
                    model.getMapHeadData(),
                    model.cachePath
                )
            } else {
                //通过自己的内部缓存机制
                mediaPlayer!!.setCache(model.isCache())
                mediaPlayer!!.setCacheDir(model.cachePath)
                mediaPlayer!!.setOverrideExtension(model.getOverrideExtension())
                mediaPlayer!!.setDataSource(
                    context,
                    model.getUrl().toUri(),
                    model.getMapHeadData()
                )
            }
            //很遗憾，EXO2的setSpeed只能在播放前生效
            if (model.getSpeed() != 1f && model.getSpeed() > 0) {
                mediaPlayer!!.setSpeed(model.getSpeed(), 1f)
            }
            // 仅在 API 29+ 使用 SurfaceControl
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                surfaceControl = SurfaceControl.Builder()
                    .setName(SURFACE_CONTROL_NAME)
                    .setBufferSize(0, 0)
                    .build()
                videoSurface = Surface(surfaceControl!!)
                mediaPlayer!!.setSurface(videoSurface)
            } else {
                mediaPlayer!!.setSurface(dummySurface)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initSuccess(model)
    }

    override fun showDisplay(msg: Message) {
        if (mediaPlayer == null) return
        if (msg.obj == null) {
            mediaPlayer!!.setSurface(dummySurface)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && msg.obj is SurfaceView) {
                reparent(msg.obj as SurfaceView?)
            } else {
                val holder: Surface? = msg.obj as Surface?
                surface = holder
                mediaPlayer!!.setSurface(holder)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun reparent(surfaceView: SurfaceView?) {
        if (surfaceControl == null) return
        if (surfaceView == null) {
            SurfaceControl.Transaction()
                .reparent(surfaceControl!!,  /* newParent= */null)
                .setBufferSize(surfaceControl!!,  /* w= */0,  /* h= */0)
                .setVisibility(surfaceControl!!,  /* visible= */false)
                .apply()
        } else {
            val newParentSurfaceControl = surfaceView.surfaceControl
            SurfaceControl.Transaction()
                .reparent(surfaceControl!!, newParentSurfaceControl)
                .setBufferSize(surfaceControl!!, surfaceView.width, surfaceView.height)
                .setVisibility(surfaceControl!!,  /* visible= */true)
                .apply()
        }
    }

    override fun setSpeed(speed: Float, soundTouch: Boolean) {
        //很遗憾，EXO2的setSpeed只能在播放前生效
        //Debuger.printfError("很遗憾，目前EXO2的setSpeed只能在播放前设置生效");
        if (mediaPlayer != null) {
            try {
                mediaPlayer!!.setSpeed(speed, 1f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun setNeedMute(needMute: Boolean) {
        if (mediaPlayer != null) {
            if (needMute) {
                mediaPlayer!!.setVolume(0f, 0f)
            } else {
                mediaPlayer!!.setVolume(1f, 1f)
            }
        }
    }

    override fun setVolume(left: Float, right: Float) {
        if (mediaPlayer != null) {
            mediaPlayer!!.setVolume(left, right)
        }
    }

    override fun releaseSurface() {
        if (surface != null) {
            //surface.release();
            surface = null
        }
    }

    /**
     * 测试异步释放
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun release() {
        if (mediaPlayer != null) {
            val mm: IjkExo2MediaPlayer? = mediaPlayer
            /** todo 测试异步，可能会收到警告
             * todo Player is accessed on the wrong thread. See https://exoplayer.dev/issues/player-accessed-on-wrong-thread */
            /*new Thread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mm.setSurface(null);
                            mm.release();

                        }

                    }
            ).start();*/
            mm!!.setSurface(null)
            mm.release()
            mediaPlayer = null
        }
        if (dummySurface != null) {
            dummySurface!!.release()
            dummySurface = null
        }

        if (surfaceControl != null) {
            surfaceControl!!.release()
            surfaceControl = null
        }
        if (videoSurface != null) {
            videoSurface!!.release()
            videoSurface = null
        }
    }

    override fun getBufferedPercentage(): Int {
        return -1
    }

    /**
     * 上一集
     */
    fun previous() {
        if (mediaPlayer == null) {
            return
        }
        mediaPlayer!!.previous()
    }

    /**
     * 下一集
     */
    fun next() {
        if (mediaPlayer == null) {
            return
        }
        mediaPlayer!!.next()
    }

    override fun getNetSpeed(): Long {
        return 0
    }

    override fun setSpeedPlaying(speed: Float, soundTouch: Boolean) {
    }

    override fun start() {
        if (mediaPlayer != null) {
            mediaPlayer!!.start()
        }
    }

    override fun stop() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
        }
    }

    override fun pause() {
        if (mediaPlayer != null) {
            mediaPlayer!!.pause()
        }
    }

    override fun getVideoWidth(): Int {
        if (mediaPlayer != null) {
            return mediaPlayer!!.videoWidth
        }
        return 0
    }

    override fun getVideoHeight(): Int {
        if (mediaPlayer != null) {
            return mediaPlayer!!.videoHeight
        }
        return 0
    }

    override fun isPlaying(): Boolean {
        if (mediaPlayer != null) {
            return mediaPlayer!!.isPlaying()
        }
        return false
    }

    override fun seekTo(time: Long) {
        if (mediaPlayer != null) {
            mediaPlayer!!.seekTo(time)
        }
    }

    override fun getCurrentPosition(): Long {
        if (mediaPlayer != null) {
            return mediaPlayer!!.getCurrentPosition()
        }
        return 0
    }

    override fun getDuration(): Long {
        if (mediaPlayer != null) {
            return mediaPlayer!!.getDuration()
        }
        return 0
    }

    override fun getVideoSarNum(): Int {
        if (mediaPlayer != null) {
            return mediaPlayer!!.videoSarNum
        }
        return 1
    }

    override fun getVideoSarDen(): Int {
        if (mediaPlayer != null) {
            return mediaPlayer!!.videoSarDen
        }
        return 1
    }

    override fun isSurfaceSupportLockCanvas(): Boolean {
        return false
    }


}
