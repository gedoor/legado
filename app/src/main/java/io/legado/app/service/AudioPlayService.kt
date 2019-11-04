package io.legado.app.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseService
import io.legado.app.constant.Action
import io.legado.app.constant.AppConst
import io.legado.app.constant.Bus
import io.legado.app.constant.Status
import io.legado.app.help.IntentHelp
import io.legado.app.help.MediaHelp
import io.legado.app.receiver.MediaButtonReceiver
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.utils.postEvent
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class AudioPlayService : BaseService(),
    AudioManager.OnAudioFocusChangeListener,
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnCompletionListener {

    companion object {
        var isRun = false
        var timeMinute: Int = 0
    }

    var pause = false
    private val handler = Handler()
    private lateinit var audioManager: AudioManager
    private var mFocusRequest: AudioFocusRequest? = null
    private var title: String = ""
    private var subtitle: String = ""
    private val mediaPlayer = MediaPlayer()
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var position = 0
    private val dsRunnable: Runnable? = Runnable { doDs() }

    override fun onCreate() {
        super.onCreate()
        isRun = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mFocusRequest = MediaHelp.getFocusRequest(this)
        mediaPlayer.setOnErrorListener(this)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        initMediaSession()
        initBroadcastReceiver()
        upNotification()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        launch(IO) {
            while (isActive) {
                delay(1000)
                postEvent(Bus.AUDIO_PROGRESS, mediaPlayer.currentPosition)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                Action.play -> {
                    title = intent.getStringExtra("title") ?: ""
                    subtitle = intent.getStringExtra("subtitle") ?: ""
                    position = intent.getIntExtra("pageIndex", 0)
                    play(intent.getStringExtra("url"))
                }
                Action.pause -> pause(true)
                Action.resume -> resume()
                Action.addTimer -> addTimer()
                Action.setTimer -> setTimer(intent.getIntExtra("minute", 0))
                else -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRun = false
        mediaPlayer.release()
        mediaSessionCompat?.release()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED)
        postEvent(Bus.AUDIO_STATE, Status.STOP)
    }

    private fun play(url: String) {
        if (requestFocus()) {
            postEvent(Bus.AUDIO_STATE, Status.PLAY)
            mediaPlayer.reset()
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
        }
    }

    private fun pause(pause: Boolean) {
        this.pause = pause
        mediaPlayer.pause()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED)
        postEvent(Bus.AUDIO_STATE, Status.PAUSE)
        upNotification()
    }

    private fun resume() {
        pause = false
        mediaPlayer.start()
        upMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        postEvent(Bus.AUDIO_STATE, Status.PLAY)
        upNotification()
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (pause) return
        mp?.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {
        postEvent(Bus.AUDIO_NEXT, 1)
    }

    private fun setTimer(minute: Int) {
        timeMinute = minute
        if (minute > 0) {
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        upNotification()
    }

    private fun addTimer() {
        if (timeMinute == 60) {
            timeMinute = 0
            handler.removeCallbacks(dsRunnable)
        } else {
            timeMinute += 10
            if (timeMinute > 60) timeMinute = 60
            handler.removeCallbacks(dsRunnable)
            handler.postDelayed(dsRunnable, 60000)
        }
        postEvent(Bus.TTS_DS, timeMinute)
        upNotification()
    }

    /**
     * 定时
     */
    private fun doDs() {
        if (!pause) {
            timeMinute--
            if (timeMinute == 0) {
                stopSelf()
            } else if (timeMinute > 0) {
                handler.postDelayed(dsRunnable, 60000)
            }
        }
        postEvent(Bus.TTS_DS, timeMinute)
        upNotification()
    }

    /**
     * 更新媒体状态
     */
    private fun upMediaSessionPlaybackState(state: Int) {
        mediaSessionCompat?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(MediaHelp.MEDIA_SESSION_ACTIONS)
                .setState(state, position.toLong(), 1f)
                .build()
        )
    }

    /**
     * 初始化MediaSession, 注册多媒体按钮
     */
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, "readAloud")
        mediaSessionCompat?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
                return MediaButtonReceiver.handleIntent(this@AudioPlayService, mediaButtonEvent)
            }
        })
        mediaSessionCompat?.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(
                    Intent.ACTION_MEDIA_BUTTON,
                    null,
                    App.INSTANCE,
                    MediaButtonReceiver::class.java
                ),
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        )
        mediaSessionCompat?.isActive = true
    }

    /**
     * 断开耳机监听
     */
    private fun initBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                    pause(true)
                }
            }
        }
        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(broadcastReceiver, intentFilter)
    }

    /**
     * 音频焦点变化
     */
    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得焦点,  可做恢复播放，恢复后台音量的操作
                if (!pause) resume()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失焦点除非重新主动获取，这种情况是被其他播放器抢去了焦点，  为避免与其他播放器混音，可将音乐暂停
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时丢失焦点，这种情况是被其他应用申请了短暂的焦点，可压低后台音量
                if (!pause) pause(false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 短暂丢失焦点，这种情况是被其他应用申请了短暂的焦点希望其他声音能压低音量（或者关闭声音）凸显这个声音（比如短信提示音），
            }
        }
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        var nTitle: String = when {
            pause -> getString(R.string.audio_pause)
            timeMinute in 1..60 -> getString(
                R.string.read_aloud_timer,
                timeMinute
            )
            else -> getString(R.string.audio_play_t)
        }
        nTitle += ": $title"
        var nSubtitle = subtitle
        if (subtitle.isEmpty()) {
            nSubtitle = getString(R.string.audio_play_s)
        }
        val builder = NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_volume_up)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_read_book))
            .setOngoing(true)
            .setContentTitle(nTitle)
            .setContentText(nSubtitle)
            .setContentIntent(
                IntentHelp.activityPendingIntent<ReadBookActivity>(this, "activity")
            )
        if (pause) {
            builder.addAction(
                R.drawable.ic_play_24dp,
                getString(R.string.resume),
                thisPendingIntent(Action.resume)
            )
        } else {
            builder.addAction(
                R.drawable.ic_pause_24dp,
                getString(R.string.pause),
                thisPendingIntent(Action.pause)
            )
        }
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.stop),
            thisPendingIntent(Action.stop)
        )
        builder.addAction(
            R.drawable.ic_time_add_24dp,
            getString(R.string.set_timer),
            thisPendingIntent(Action.addTimer)
        )
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSessionCompat?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(112201, notification)
    }

    /**
     * @return 音频焦点
     */
    private fun requestFocus(): Boolean {
        return MediaHelp.requestFocus(audioManager, this, mFocusRequest)
    }

    private fun thisPendingIntent(action: String): PendingIntent? {
        return IntentHelp.servicePendingIntent<AudioPlayService>(this, action)
    }
}