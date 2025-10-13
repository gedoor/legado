package io.legado.app.help.gsyVideo

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener
import com.shuyu.gsyvideoplayer.render.GSYRenderView
import com.shuyu.gsyvideoplayer.utils.Debuger
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import io.legado.app.R
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.externalCache
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File


class VideoPlayer: StandardGSYVideoPlayer {
    constructor(context: Context?, fullFlag: Boolean?) : super(context, fullFlag) //必须的,全屏时依靠这个构建知道获取全屏布局
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    private var mSwitchSize: TextView? = null
    private var isChanging = false
    private var mUrlList : List<BookChapter>? =  null
    private var source: BaseSource? = null
    private var book: Book? = null
    private var mSourcePosition = 0
    private var mPreSourcePosition = 0
    private var mTypeText: String? = "标准"
    private var mTmpManager: GSYVideoManager? = null
    private var mLoadingDialog: LoadingDialog? = null
    private var mExoCache: Boolean = false


    override fun init(context: Context) {
        super.init(context)
        mUrlList = ExoVideoManager.mUrlList
        source = ExoVideoManager.mSource
        book = ExoVideoManager.mBook
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
                        super.onLongPress(e)
                    }
                }
            )
        }
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
        getGSYVideoManager().setPlayTag(mPlayTag)
        getGSYVideoManager().setPlayPosition(mPlayPosition)
        // Audio focus is now handled by the base class GSYAudioFocusManager
        try {
            //可以删去，在调用程序那边处理
            (activityContext as Activity).window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mBackUpPlayingBufferState = -1

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



    private fun initView() {
        mSwitchSize = findViewById(R.id.switchSize)
        //切换选集
        mSwitchSize?.setOnClickListener {
            if (mHadPlay && !isChanging) {
                showSwitchDialog()
            }
        }
    }
    private fun showSwitchDialog() {
        if (!mHadPlay || mUrlList.isNullOrEmpty()) {
            return
        }
        val switchVideoTypeDialog = SwitchVideoTypeDialog(getContext())
        switchVideoTypeDialog.initList(mUrlList!!, object :
            SwitchVideoTypeDialog.OnListItemClickListener {
            override fun onItemClick(position: Int) {
                resolveStartChange(position)
            }
        })
        switchVideoTypeDialog.show()
    }
    private val gsyMediaPlayerListener: GSYMediaPlayerListener = object : GSYMediaPlayerListener {
        override fun onPrepared() {
            if (mTmpManager != null) {
                mTmpManager!!.start()
                mTmpManager!!.seekTo(getCurrentPositionWhenPlaying())
            }
        }

        override fun onAutoCompletion() {}
        override fun onCompletion() {}
        override fun onBufferingUpdate(percent: Int) {}

        override fun onSeekComplete() {
            if (mTmpManager != null) {
                val manager = ExoVideoManager.instance()
                GSYVideoManager.changeManager(mTmpManager)
                mTmpManager!!.setLastListener(manager.lastListener())
                mTmpManager!!.setListener(manager.listener())

                manager.setDisplay(null)

                Debuger.printfError("**** showDisplay onSeekComplete ***** " + mSurface)
                Debuger.printfError("**** showDisplay onSeekComplete isValid***** " + mSurface.isValid())
                mTmpManager!!.setDisplay(mSurface)
                changeUiToPlayingClear()
                resolveChangedResult()
                manager.releaseMediaPlayer()
            }
        }

        override fun onError(what: Int, extra: Int) {
            mSourcePosition = mPreSourcePosition
            if (mTmpManager != null) {
                mTmpManager!!.releaseMediaPlayer()
            }
            post {
                resolveChangedResult()
                Toast.makeText(mContext, "change Fail", Toast.LENGTH_LONG).show()
            }
        }

        override fun onInfo(what: Int, extra: Int) {}
        override fun onVideoSizeChanged() {}
        override fun onBackFullscreen() {}
        override fun onVideoPause() {}
        override fun onVideoResume() {}
        override fun onVideoResume(seek: Boolean) {}
    }

    private fun resolveChangedResult() {
        isChanging = false
        mTmpManager = null
        val name: String? = mUrlList!!.get(mSourcePosition).title
        val url: String? = getUrl(mSourcePosition)
        mTypeText = name
        mSwitchSize!!.text = name
        resolveChangeUrl(mCache, mCachePath, url)
        hideLoading()
    }
    private fun resolveStartChange(position: Int) {
        val name: String? = mUrlList!!.get(position).title
        if (mSourcePosition != position) {
            if ((mCurrentState == CURRENT_STATE_PLAYING
                        || mCurrentState == CURRENT_STATE_PAUSE)
            ) {
                showLoading()
                val url: String? = getUrl(position)
                cancelProgressTimer()
                hideAllWidget()
                if (mTitle != null && mTitleTextView != null) {
                    mTitleTextView.setText(mTitle)
                }
                mPreSourcePosition = mSourcePosition
                isChanging = true
                mTypeText = name
                mSwitchSize!!.setText(name)
                mSourcePosition = position
                //创建临时管理器执行加载播放
                mTmpManager = GSYVideoManager.tmpInstance(gsyMediaPlayerListener)
                mTmpManager!!.initContext(getContext().getApplicationContext())
                resolveChangeUrl(mCache, mCachePath, url)
                mTmpManager!!.prepare(mUrl, mMapHeadData, mLooping, mSpeed, mCache, mCachePath, null)
                changeUiToPlayingBufferingShow()
            }
        } else {
            Toast.makeText(getContext(), "已经是 " + name, Toast.LENGTH_LONG).show()
        }
    }
    private fun resolveChangeUrl(cacheWithPlay: Boolean, cachePath: File?, url: String?) {
        if (mTmpManager != null) {
            mCache = cacheWithPlay
            mCachePath = cachePath
            mOriginUrl = url
            this.mUrl = url
        }
    }

    private fun showLoading() {
        hideLoading()
        mLoadingDialog = LoadingDialog(mContext)
        mLoadingDialog!!.show()
    }
    private fun hideLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog!!.dismiss()
            mLoadingDialog = null
        }
    }

    //重写，非全屏时16/9
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        if (mIfCurrentIsFullscreen) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//        } else {
//            val width = MeasureSpec.getSize(widthMeasureSpec)
//            val height = width * 9 / 16
//            super.onMeasure(
//            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
//            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
//            )
//        }
//    }


    override fun getLayoutId(): Int {
        return if (mIfCurrentIsFullscreen)
            R.layout.video_layout_controller_full
        else R.layout.video_layout_controller
    }
    fun getUrl(position: Int): String? {
        if (mUrlList == null || source == null || book == null) return null
        val chapter = mUrlList!!.getOrNull(position) ?: return null
//        val videoUrl = WebBook.getContentAwait(source as BookSource, book!!, chapter)
        val videoUrl = runBlocking {
            WebBook.getContentAwait(source as BookSource, book!!, chapter)
        }
        val analyzeUrl = AnalyzeUrl(
            videoUrl,
            source = source,
            ruleData = book,
            chapter = chapter
        )
        mapHeadData = analyzeUrl.headerMap
        return analyzeUrl.url
    }

    fun setUp(toc:List<BookChapter>?, source: BaseSource?, book: Book?): Boolean {
        if (toc == null || source == null || book == null) return false
        this.source = source
        ExoVideoManager.mSource = source
        this.book = book
        ExoVideoManager.mBook = book
        book.durChapterPos.takeIf { it > 0 }?.toLong()?.let { seekOnStart = it }
        mUrlList = listOf(toc[book.durChapterIndex])
        ExoVideoManager.mUrlList = mUrlList
        return super.setUp(mUrlList!![0].url, false, File(appCtx.externalCache, "exoplayer"), toc[mSourcePosition].title)
    }


    /**********以下重载GSYVideoPlayer的GSYVideoViewBridge相关实现***********/
    override fun getGSYVideoManager(): ExoVideoManager {
        ExoVideoManager.instance().initContext(context.applicationContext)
        return ExoVideoManager.instance()
    }
    public override fun backFromFull(context: Context?): Boolean {
        return ExoVideoManager.backFromWindowFull(context)
    }
    override fun releaseVideos() {
        ExoVideoManager.releaseAllVideos()
    }

    override fun getFullId(): Int {
        return ExoVideoManager.FULLSCREEN_ID
    }

    override fun getSmallId(): Int {
        return ExoVideoManager.SMALL_ID
    }
//    override fun onInfo(what: Int, extra: Int) {
//        if (what == POSITION_DISCONTINUITY) {
//            val window: Int = (getGSYVideoManager().player.mediaPlayer as Exo2MediaPlayer).currentWindowIndex
//            mPlayPosition = window
//            val gsyVideoModel: BookChapter = mUrlList?.get(window)!!
//            if (!TextUtils.isEmpty(gsyVideoModel.title)) {
//                mTitleTextView.text = gsyVideoModel.title
//            }
//        } else {
//            super.onInfo(what, extra)
//        }
//    }
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

    fun saveState(): VideoPlayer {
//        val switchVideo = VideoPlayer(context)
//        val switchVideo = VideoPlayer(appCtx)
//        cloneParams(this, switchVideo)
//        return switchVideo
        return this
    }

    fun cloneState(switchVideo: StandardGSYVideoPlayer) {
        cloneParams(switchVideo, this)
    }
}