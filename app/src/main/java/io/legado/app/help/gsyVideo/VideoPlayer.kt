package io.legado.app.help.gsyVideo

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.TextView
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import io.legado.app.R
import io.legado.app.model.VideoPlay


class VideoPlayer: StandardGSYVideoPlayer {
    constructor(context: Context?, fullFlag: Boolean?) : super(context, fullFlag) //必须的,全屏时依靠这个构建知道获取全屏布局
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private var mSwitchSize: TextView? = null
    private var btnNext: ImageView? = null
    private var isChanging = false


    override fun init(context: Context) {
        super.init(context)
        initView()
        post {
            gestureDetector = GestureDetector(
                getContext().applicationContext,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        touchDoubleUp(e)
                        return super.onDoubleTap(e)
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        if (!mChangePosition && !mChangeVolume && !mBrightness && mCurrentState != CURRENT_STATE_ERROR
                        ) {
                            onClickUiToggle(e)
                        }
                        return super.onSingleTapConfirmed(e)
                    }

                    override fun onLongPress(e: MotionEvent) {
                        setSpeed(3f, true)
                        super.onLongPress(e)
                    }
                }
            )
        }
    }
    override fun touchSurfaceUp(){
        speed = 1f
        super.touchSurfaceUp()
    }


    private fun initView() {
        mSwitchSize = findViewById(R.id.switchSize)
        btnNext = findViewById(R.id.next)
        if (VideoPlay.episodes == null) {
            mSwitchSize?.visibility = GONE
            btnNext?.visibility = GONE
            return
        }
        //切换选集
        mSwitchSize?.setOnClickListener {
            if (mHadPlay && !isChanging) {
                showSwitchDialog()
            }
        }
        btnNext?.setOnClickListener {
            if (VideoPlay.upDurIndex(1)) {
                VideoPlay.saveRead()
                VideoPlay.startPlay(this)
            }
        }

    }
    private fun showSwitchDialog() {
        if (!mHadPlay || VideoPlay.episodes.isNullOrEmpty()) {
            return
        }
        isChanging = true
        val switchVideoTypeDialog = SwitchVideoTypeDialog(context)
        switchVideoTypeDialog.initList(VideoPlay.episodes!!, object :
            SwitchVideoTypeDialog.OnListItemClickListener {
            override fun onItemClick(position: Int) {
                VideoPlay.chapterInVolumeIndex = position
                VideoPlay.durChapterPos = 0
                VideoPlay.saveRead()
                VideoPlay.startPlay(this@VideoPlayer)
            }

            override fun finishDialog() {
                isChanging = false
            }
        })
        switchVideoTypeDialog.show()
    }

    override fun getLayoutId(): Int {
        return if (mIfCurrentIsFullscreen)
            R.layout.video_layout_controller_full
        else R.layout.video_layout_controller
    }

    override fun updateStartImage() {
        if (mIfCurrentIsFullscreen) {
            if (mStartButton is ImageView) {
                val imageView = mStartButton as ImageView
                when (mCurrentState) {
                    CURRENT_STATE_PLAYING -> {
                        imageView.setImageResource(R.drawable.ic_pause_24dp)
                    }
                    CURRENT_STATE_ERROR -> {
                        imageView.setImageResource(R.drawable.ic_pause_outline_24dp)
                    }
                    else -> {
                        imageView.setImageResource(R.drawable.ic_play_24dp)
                    }
                }
            }
        } else {
            super.updateStartImage()
        }
    }


    /**********以下重载GSYVideoPlayer的GSYVideoViewBridge相关实现***********/
    override fun getGSYVideoManager(): ExoVideoManager {
        return VideoPlay.videoManager.apply { initContext(context.applicationContext) }
    }
    public override fun backFromFull(context: Context?): Boolean {
        return VideoPlay.backFromWindowFull(context)
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

    fun saveState(): VideoPlayer {
        return this
    }

    fun cloneState(switchVideo: StandardGSYVideoPlayer) {
        cloneParams(switchVideo, this)
    }
}