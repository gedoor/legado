package io.legado.app.help.gsyVideo

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.view.isInvisible
import androidx.core.view.isNotEmpty
import com.shuyu.gsyvideoplayer.render.GSYRenderView
import com.shuyu.gsyvideoplayer.utils.Debuger
import com.shuyu.gsyvideoplayer.utils.NetworkUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import io.legado.app.R
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.externalCache
import splitties.init.appCtx
import java.io.File
import com.shuyu.gsyvideoplayer.R as gsyR


class FloatingPlayer : StandardGSYVideoPlayer {
    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var mUrlList: List<BookChapter>? =  null
    private var source: BaseSource? = null
    private var book: Book? = null
    private var mSourcePosition = 0
    private var mExoCache: Boolean = false
    lateinit var fullscreenB: ImageView

    override fun init(context: Context?) {
        mUrlList = ExoVideoManager.mUrlList
        source = ExoVideoManager.mSource
        book = ExoVideoManager.mBook
        if (activityContext != null) {
            this.mContext = activityContext
        } else {
            this.mContext = context
        }
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

    override fun startPrepare() {
        if (getGSYVideoManager().listener() != null) {
            getGSYVideoManager().listener().onCompletion()
        }
        if (mVideoAllCallBack != null) {
            Debuger.printfLog("onStartPrepared")
            mVideoAllCallBack.onStartPrepared(mOriginUrl, mTitle, this)
        }
        getGSYVideoManager().setListener(this)
        getGSYVideoManager().playTag = mPlayTag
        getGSYVideoManager().playPosition = mPlayPosition
        mBackUpPlayingBufferState = -1
        //getGSYVideoManager().prepare(mUrl, mMapHeadData, mLooping, mSpeed, mCache, mCachePath, null)

        //prepare通过list初始化
        if (mUrlList.isNullOrEmpty()) {
            Debuger.printfError("********************** urls isEmpty . Do you know why ? **********************")
            return
        }
        if (book == null || source !is BookSource) {
            Debuger.printfError("********************** no book no bookSource **********************")
            return
        }
        getGSYVideoManager().prepare(
            mUrlList!!,
            book!!,
            source as BookSource,
            if (mMapHeadData == null) HashMap() else mMapHeadData,
            mPlayPosition,
            mLooping,
            mSpeed,
            mExoCache,
            mCachePath,
            mOverrideExtension
        )
        setStateAndUi(CURRENT_STATE_PREPAREING)
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
            getGSYVideoManager().setListener(null)
            getGSYVideoManager().setLastListener(null)
        }
        getGSYVideoManager().currentVideoHeight = 0
        getGSYVideoManager().currentVideoWidth = 0
        releaseNetWorkState()
    }

    override fun getActivityContext(): Context? {
        return context
    }

    override fun isShowNetConfirm(): Boolean {
        return false
    }

    override fun showWifiDialog() {
        if (!NetworkUtils.isAvailable(mContext)) {
            startPlayLogic()
            return
        }
        val builder = AlertDialog.Builder(activityContext)
        builder.setMessage(resources.getString(gsyR.string.tips_not_wifi))
        builder.setPositiveButton(
            resources.getString(gsyR.string.tips_not_wifi_confirm)
        ) { dialog, which ->
            dialog.dismiss()
            startPlayLogic()
        }
        builder.setNegativeButton(
            resources.getString(gsyR.string.tips_not_wifi_cancel)
        ) { dialog, which -> dialog.dismiss() }
        val alertDialog = builder.create()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            alertDialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        } else {
            alertDialog.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        }
        alertDialog.show()
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

    fun setUp(toc:List<BookChapter>?, source: BaseSource?, book: Book?): Boolean {
        if (toc == null || source == null || book == null) return false
        mUrlList = toc.take(1)
        ExoVideoManager.mUrlList = mUrlList
        this.source = source
        ExoVideoManager.mSource = source
        this.book = book
        ExoVideoManager.mBook = book
        return super.setUp(mUrlList!![0].url, false, File(appCtx.externalCache, "exoplayer"), toc[mSourcePosition].title)
    }


    /**********以下重载GSYVideoPlayer的GSYVideoViewBridge相关实现***********/
    override fun getGSYVideoManager(): ExoVideoManager {
        ExoVideoManager.instance().initContext(context.applicationContext)
        return ExoVideoManager.instance()
    }
    override fun releaseVideos() {
        ExoVideoManager.releaseAllVideos()
    }
    override fun setDisplay(surface: Surface?) {
        if (surface != null && mTextureView.getShowView() is SurfaceView) {
            val surfaceView = (mTextureView.getShowView() as SurfaceView?)
            getGSYVideoManager().setDisplayNew(surfaceView)
        } else if (surface != null) {
            getGSYVideoManager().setDisplay(surface)
        } else {
            getGSYVideoManager().setDisplayNew(null)
        }
    }
    fun nextUI() { resetProgressAndTime() }

    //播放器转移
    fun setSurfaceToPlay() {
        addTextureView()
        getGSYVideoManager().setListener(this)
        checkoutState()
    }
    fun setSurfaceToPlay2() {
        if (mTextureView == null) {
            mTextureView = GSYRenderView()
        }
        mTextureView.addView(
            context,
            mTextureViewContainer,
            mRotate,
            this,
            this,
            mEffectFilter,
            mMatrixGL,
            mRenderer,
            mMode
        )
        getGSYVideoManager().setListener(this)
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
//        val switchVideo = FloatingPlayer(context)
//        val switchVideo = FloatingPlayer(appCtx)
//        cloneParams(this, switchVideo)
//        return switchVideo
        return this
    }

    fun cloneState(switchVideo: StandardGSYVideoPlayer) {
        cloneParams(switchVideo, this)
    }
}