package io.legado.app.help.gsyVideo

import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceView
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isNotEmpty
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import io.legado.app.R
import io.legado.app.model.VideoPlay


class FloatingPlayer : StandardGSYVideoPlayer {
    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    lateinit var fullscreenB: ImageView

    override fun init(context: Context?) {
        if (activityContext != null) {
            this.mContext = activityContext
        } else {
            this.mContext = context
        }
        mAudioManager = mContext.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        initInflate(mContext)
        mTextureViewContainer = findViewById(R.id.surface_container)
        if (isInEditMode) return
        mScreenWidth = activityContext!!.resources.displayMetrics.widthPixels
        mScreenHeight = activityContext!!.resources.displayMetrics.heightPixels
        mStartButton = findViewById(R.id.start)
        mStartButton.setOnClickListener { clickStartIcon() }
        mTopContainer = findViewById(R.id.layout_top)
        mBackButton = findViewById(R.id.back)
        mBottomProgressBar = findViewById(R.id.bottom_progressbar)
        fullscreenB = findViewById(R.id.fullscreenB)
    }

    override fun getLayoutId(): Int {
        return R.layout.video_layout_floating
    }


    override fun onAutoCompletion() {
        setStateAndUi(CURRENT_STATE_AUTO_COMPLETE)
        mSaveChangeViewTIme = 0
        if (mTextureViewContainer.isNotEmpty()) {
            mTextureViewContainer.removeAllViews()
        }
        if (!mIfCurrentIsFullscreen) getGSYVideoManager().setLastListener(null)
        releaseNetWorkState()
        if (mVideoAllCallBack != null && isCurrentMediaListener) {
            mVideoAllCallBack.onAutoComplete(mOriginUrl, mTitle, this)
        }
    }

    override fun onCompletion() {
        setStateAndUi(CURRENT_STATE_NORMAL)
        mSaveChangeViewTIme = 0
        if (mTextureViewContainer.isNotEmpty()) {
            mTextureViewContainer.removeAllViews()
        }
        if (!mIfCurrentIsFullscreen) {
            gsyVideoManager.setListener(null)
            gsyVideoManager.setLastListener(null)
        }
        gsyVideoManager.currentVideoHeight = 0
        gsyVideoManager.currentVideoWidth = 0
        releaseNetWorkState()
    }

    override fun getActivityContext(): Context? {
        return context
    }

    override fun isShowNetConfirm(): Boolean {
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun setProgressAndTime(
        progress: Long, secProgress: Long, currentTime: Long, totalTime: Long, forceChange: Boolean
    ) {
        if (mHadSeekTouch) {
            return
        }
        if (mBottomProgressBar != null) {
            if (progress != 0L || forceChange) mBottomProgressBar.progress = progress.toInt()
        }
    }

    fun showControlUi() {
        if (mStartButton.isInvisible) {
            resolveUIState(mCurrentState)
        } else {
            hideAllWidget()
        }
    }

    override fun getFullWindowPlayer(): GSYVideoPlayer? = null
    override fun getSmallWindowPlayer(): GSYVideoPlayer? = null

    /**********以下重载GSYVideoPlayer的GSYVideoViewBridge相关实现***********/
    override fun getGSYVideoManager(): ExoVideoManager {
        return VideoPlay.videoManager.apply { initContext(context.applicationContext) }
    }
    override fun releaseVideos() {
        VideoPlay.releaseAllVideos()
    }
    override fun getFullId(): Int {
        return ExoVideoManager.FULLSCREEN_ID
    }

    override fun getSmallId(): Int {
        return ExoVideoManager.SMALL_ID
    }
    override fun setDisplay(surface: Surface?) {
        if (surface != null && mTextureView.getShowView() is SurfaceView) {
            val surfaceView = (mTextureView.getShowView() as SurfaceView?)
            gsyVideoManager.setDisplayNew(surfaceView)
        } else if (surface != null) {
            gsyVideoManager.setDisplay(surface)
        } else {
            gsyVideoManager.setDisplayNew(null)
        }
    }
    fun nextUI() { resetProgressAndTime() }

    //播放器转移
    fun setSurfaceToPlay() {
        addTextureView()
        gsyVideoManager.setListener(this)
        checkoutState()
    }

    var needDestroy: Boolean = true

    override fun onSurfaceDestroyed(surface: Surface?): Boolean {
        if (needDestroy) {
            return super.onSurfaceDestroyed(surface)
        } else {
            releaseSurface(surface)
            needDestroy = true
            return true
        }
    }

    fun saveState(): FloatingPlayer {
        return this
    }

    fun cloneState(switchVideo: StandardGSYVideoPlayer) {
        cloneParams(switchVideo, this)
    }
}