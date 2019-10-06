package io.legado.app.ui.widget.page.curl

import android.content.Context
import android.graphics.PointF
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OpenGL ES View.
 *
 * @author harism
 */
class CurlView : GLSurfaceView, View.OnTouchListener, CurlRenderer.Observer {

    private var mAllowLastPageCurl = true

    private var mAnimate = false
    private val mAnimationDurationTime: Long = 300
    private val mAnimationSource = PointF()
    private var mAnimationStartTime: Long = 0
    private val mAnimationTarget = PointF()
    private var mAnimationTargetEvent: Int = 0

    private val mCurlDir = PointF()

    private val mCurlPos = PointF()
    private var mCurlState = CURL_NONE
    // Current bitmap index. This is always showed as front of right page.
    private var mCurrentIndex = 0

    // Start position for dragging.
    private val mDragStartPos = PointF()

    private var mEnableTouchPressure = false
    // Bitmap size. These are updated from renderer once it's initialized.
    private var mPageBitmapHeight = -1

    private var mPageBitmapWidth = -1
    // Page meshes. Left and right meshes are 'static' while curl is used to
    // show page flipping.
    private var mPageCurl: CurlMesh
    private var mPageLeft: CurlMesh
    private var mPageRight: CurlMesh

    private val mPointerPos = PointerPosition()

    private var mRenderer: CurlRenderer = CurlRenderer(this)
    private var mRenderLeftPage = true
    private var mSizeChangedObserver: SizeChangedObserver? = null

    // One page is the default.
    private var mViewMode = SHOW_ONE_PAGE

    var mPageProvider: PageProvider? = null
        set(value) {
            field = value
            mCurrentIndex = 0
            updatePages()
            requestRender()
        }

    /**
     * Get current page index. Page indices are zero based values presenting
     * page being shown on right side of the book.
     */
    /**
     * Set current page index. Page indices are zero based values presenting
     * page being shown on right side of the book. E.g if you set value to 4;
     * right side front facing bitmap will be with index 4, back facing 5 and
     * for left side page index 3 is front facing, and index 2 back facing (once
     * page is on left side it's flipped over).
     *
     *
     * Current index is rounded to closest value divisible with 2.
     */
    var currentIndex: Int
        get() = mCurrentIndex
        set(index) {
            mCurrentIndex = if (mPageProvider == null || index < 0) {
                0
            } else {
                if (mAllowLastPageCurl) {
                    min(index, mPageProvider!!.pageCount)
                } else {
                    min(index, mPageProvider!!.pageCount - 1)
                }
            }
            updatePages()
            requestRender()
        }

    /**
     * Default constructor.
     */
    constructor(ctx: Context) : super(ctx)

    /**
     * Default constructor.
     */
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    /**
     * Default constructor.
     */
    constructor(ctx: Context, attrs: AttributeSet, defStyle: Int) : this(ctx, attrs)

    /**
     * Initialize method.
     */
    init {
        setRenderer(mRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        setOnTouchListener(this)

        // Even though left and right pages are static we have to allocate room
        // for curl on them too as we are switching meshes. Another way would be
        // to swap texture ids only.
        mPageLeft = CurlMesh(10)
        mPageRight = CurlMesh(10)
        mPageCurl = CurlMesh(10)
        mPageLeft.setFlipTexture(true)
        mPageRight.setFlipTexture(false)
    }

    override fun onDrawFrame() {
        // We are not animating.
        if (!mAnimate) {
            return
        }

        val currentTime = System.currentTimeMillis()
        // If animation is done.
        if (currentTime >= mAnimationStartTime + mAnimationDurationTime) {
            if (mAnimationTargetEvent == SET_CURL_TO_RIGHT) {
                // Switch curled page to right.
                val right = mPageCurl
                val curl = mPageRight
                right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!)
                right.setFlipTexture(false)
                right.reset()
                mRenderer.removeCurlMesh(curl)
                mPageCurl = curl
                mPageRight = right
                // If we were curling left page update current index.
                if (mCurlState == CURL_LEFT) {
                    --mCurrentIndex
                }
            } else if (mAnimationTargetEvent == SET_CURL_TO_LEFT) {
                // Switch curled page to left.
                val left = mPageCurl
                val curl = mPageLeft
                left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
                left.setFlipTexture(true)
                left.reset()
                mRenderer.removeCurlMesh(curl)
                if (!mRenderLeftPage) {
                    mRenderer.removeCurlMesh(left)
                }
                mPageCurl = curl
                mPageLeft = left
                // If we were curling right page update current index.
                if (mCurlState == CURL_RIGHT) {
                    ++mCurrentIndex
                }
            }
            mCurlState = CURL_NONE
            mAnimate = false
            requestRender()
        } else {
            mPointerPos.mPos.set(mAnimationSource)
            var t = 1f - (currentTime - mAnimationStartTime).toFloat() / mAnimationDurationTime
            t = 1f - t * t * t * (3 - 2 * t)
            mPointerPos.mPos.x += (mAnimationTarget.x - mAnimationSource.x) * t
            mPointerPos.mPos.y += (mAnimationTarget.y - mAnimationSource.y) * t
            updateCurlPos(mPointerPos)
        }
    }

    override fun onPageSizeChanged(width: Int, height: Int) {
        mPageBitmapWidth = width
        mPageBitmapHeight = height
        updatePages()
        requestRender()
    }

    public override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        requestRender()
        if (mSizeChangedObserver != null) {
            mSizeChangedObserver!!.onSizeChanged(w, h)
        }
    }

    override fun onSurfaceCreated() {
        // In case surface is recreated, let page meshes drop allocated texture
        // ids and ask for new ones. There's no need to set textures here as
        // onPageSizeChanged should be called later on.
        mPageLeft.resetTexture()
        mPageRight.resetTexture()
        mPageCurl.resetTexture()
    }

    override fun onTouch(view: View, me: MotionEvent): Boolean {
        // No dragging during animation at the moment.
        if (mAnimate || mPageProvider == null) {
            return false
        }

        // We need page rects quite extensively so get them for later use.
        val rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)
        val leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)

        // Store pointer position.
        mPointerPos.mPos.set(me.x, me.y)
        mRenderer.translate(mPointerPos.mPos)
        if (mEnableTouchPressure) {
            mPointerPos.mPressure = me.pressure
        } else {
            mPointerPos.mPressure = 0.8f
        }

        when (me.action) {
            MotionEvent.ACTION_DOWN -> {
                run {

                    // Once we receive pointer down event its position is mapped to
                    // right or left edge of page and that'll be the position from where
                    // user is holding the paper to make curl happen.
                    mDragStartPos.set(mPointerPos.mPos)

                    // First we make sure it's not over or below page. Pages are
                    // supposed to be same height so it really doesn't matter do we use
                    // left or right one.
                    if (mDragStartPos.y > rightRect!!.top) {
                        mDragStartPos.y = rightRect.top
                    } else if (mDragStartPos.y < rightRect.bottom) {
                        mDragStartPos.y = rightRect.bottom
                    }

                    // Then we have to make decisions for the user whether curl is going
                    // to happen from left or right, and on which page.
                    if (mViewMode == SHOW_TWO_PAGES) {
                        // If we have an open book and pointer is on the left from right
                        // page we'll mark drag position to left edge of left page.
                        // Additionally checking mCurrentIndex is higher than zero tells
                        // us there is a visible page at all.
                        if (mDragStartPos.x < rightRect.left && mCurrentIndex > 0) {
                            mDragStartPos.x = leftRect!!.left
                            startCurl(CURL_LEFT)
                        } else if (mDragStartPos.x >= rightRect.left && mCurrentIndex < mPageProvider!!.pageCount) {
                            mDragStartPos.x = rightRect.right
                            if (!mAllowLastPageCurl && mCurrentIndex >= mPageProvider!!.pageCount - 1) {
                                return false
                            }
                            startCurl(CURL_RIGHT)
                        }// Otherwise check pointer is on right page's side.
                    } else if (mViewMode == SHOW_ONE_PAGE) {
                        val halfX = (rightRect.right + rightRect.left) / 2
                        if (mDragStartPos.x < halfX && mCurrentIndex > 0) {
                            mDragStartPos.x = rightRect.left
                            startCurl(CURL_LEFT)
                        } else if (mDragStartPos.x >= halfX && mCurrentIndex < mPageProvider!!.pageCount) {
                            mDragStartPos.x = rightRect.right
                            if (!mAllowLastPageCurl && mCurrentIndex >= mPageProvider!!.pageCount - 1) {
                                return false
                            }
                            startCurl(CURL_RIGHT)
                        }
                    }
                    // If we have are in curl state, let this case clause flow through
                    // to next one. We have pointer position and drag position defined
                    // and this will create first render request given these points.
                    if (mCurlState == CURL_NONE) {
                        return false
                    }
                }
                updateCurlPos(mPointerPos)
            }
            MotionEvent.ACTION_MOVE -> {
                updateCurlPos(mPointerPos)
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
                    // Animation source is the point from where animation starts.
                    // Also it's handled in a way we actually simulate touch events
                    // meaning the output is exactly the same as if user drags the
                    // page to other side. While not producing the best looking
                    // result (which is easier done by altering curl position and/or
                    // direction directly), this is done in a hope it made code a
                    // bit more readable and easier to maintain.
                    mAnimationSource.set(mPointerPos.mPos)
                    mAnimationStartTime = System.currentTimeMillis()

                    // Given the explanation, here we decide whether to simulate
                    // drag to left or right end.
                    if (mViewMode == SHOW_ONE_PAGE && mPointerPos.mPos.x > (rightRect!!.left + rightRect.right) / 2 || mViewMode == SHOW_TWO_PAGES && mPointerPos.mPos.x > rightRect!!.left) {
                        // On right side target is always right page's right border.
                        mAnimationTarget.set(mDragStartPos)
                        mAnimationTarget.x = mRenderer
                            .getPageRect(CurlRenderer.PAGE_RIGHT)!!.right
                        mAnimationTargetEvent = SET_CURL_TO_RIGHT
                    } else {
                        // On left side target depends on visible pages.
                        mAnimationTarget.set(mDragStartPos)
                        if (mCurlState == CURL_RIGHT || mViewMode == SHOW_TWO_PAGES) {
                            mAnimationTarget.x = leftRect!!.left
                        } else {
                            mAnimationTarget.x = rightRect!!.left
                        }
                        mAnimationTargetEvent = SET_CURL_TO_LEFT
                    }
                    mAnimate = true
                    requestRender()
                }
            }
        }

        return true
    }

    /**
     * Allow the last page to curl.
     */
    fun setAllowLastPageCurl(allowLastPageCurl: Boolean) {
        mAllowLastPageCurl = allowLastPageCurl
    }

    /**
     * Sets mPageCurl curl position.
     */
    private fun setCurlPos(curlPos: PointF, curlDir: PointF, radius: Double) {

        // First reposition curl so that page doesn't 'rip off' from book.
        if (mCurlState == CURL_RIGHT || mCurlState == CURL_LEFT && mViewMode == SHOW_ONE_PAGE) {
            val pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)
            if (curlPos.x >= pageRect!!.right) {
                mPageCurl.reset()
                requestRender()
                return
            }
            if (curlPos.x < pageRect.left) {
                curlPos.x = pageRect.left
            }
            if (curlDir.y != 0f) {
                val diffX = curlPos.x - pageRect.left
                val leftY = curlPos.y + diffX * curlDir.x / curlDir.y
                if (curlDir.y < 0 && leftY < pageRect.top) {
                    curlDir.x = curlPos.y - pageRect.top
                    curlDir.y = pageRect.left - curlPos.x
                } else if (curlDir.y > 0 && leftY > pageRect.bottom) {
                    curlDir.x = pageRect.bottom - curlPos.y
                    curlDir.y = curlPos.x - pageRect.left
                }
            }
        } else if (mCurlState == CURL_LEFT) {
            val pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)
            if (curlPos.x <= pageRect!!.left) {
                mPageCurl.reset()
                requestRender()
                return
            }
            if (curlPos.x > pageRect.right) {
                curlPos.x = pageRect.right
            }
            if (curlDir.y != 0f) {
                val diffX = curlPos.x - pageRect.right
                val rightY = curlPos.y + diffX * curlDir.x / curlDir.y
                if (curlDir.y < 0 && rightY < pageRect.top) {
                    curlDir.x = pageRect.top - curlPos.y
                    curlDir.y = curlPos.x - pageRect.right
                } else if (curlDir.y > 0 && rightY > pageRect.bottom) {
                    curlDir.x = curlPos.y - pageRect.bottom
                    curlDir.y = pageRect.right - curlPos.x
                }
            }
        }

        // Finally normalize direction vector and do rendering.
        val dist = sqrt((curlDir.x * curlDir.x + curlDir.y * curlDir.y).toDouble())
        if (dist != 0.0) {
            curlDir.x /= dist.toFloat()
            curlDir.y /= dist.toFloat()
            mPageCurl.curl(curlPos, curlDir, radius)
        } else {
            mPageCurl.reset()
        }

        requestRender()
    }

    /**
     * If set to true, touch event pressure information is used to adjust curl
     * radius. The more you press, the flatter the curl becomes. This is
     * somewhat experimental and results may vary significantly between devices.
     * On emulator pressure information seems to be flat 1.0f which is maximum
     * value and therefore not very much of use.
     */
    fun setEnableTouchPressure(enableTouchPressure: Boolean) {
        mEnableTouchPressure = enableTouchPressure
    }

    /**
     * Set margins (or padding). Note: margins are proportional. Meaning a value
     * of .1f will produce a 10% margin.
     */
    fun setMargins(left: Float, top: Float, right: Float, bottom: Float) {
        mRenderer.setMargins(left, top, right, bottom)
    }

    /**
     * Setter for whether left side page is rendered. This is useful mostly for
     * situations where right (main) page is aligned to left side of screen and
     * left page is not visible anyway.
     */
    fun setRenderLeftPage(renderLeftPage: Boolean) {
        mRenderLeftPage = renderLeftPage
    }

    /**
     * Sets SizeChangedObserver for this View. Call back method is called from
     * this View's onSizeChanged method.
     */
    fun setSizeChangedObserver(observer: SizeChangedObserver) {
        mSizeChangedObserver = observer
    }

    /**
     * Sets view mode. Value can be either SHOW_ONE_PAGE or SHOW_TWO_PAGES. In
     * former case right page is made size of display, and in latter case two
     * pages are laid on visible area.
     */
    fun setViewMode(viewMode: Int) {
        when (viewMode) {
            SHOW_ONE_PAGE -> {
                mViewMode = viewMode
                mPageLeft.setFlipTexture(true)
                mRenderer.setViewMode(CurlRenderer.SHOW_ONE_PAGE)
            }
            SHOW_TWO_PAGES -> {
                mViewMode = viewMode
                mPageLeft.setFlipTexture(false)
                mRenderer.setViewMode(CurlRenderer.SHOW_TWO_PAGES)
            }
        }
    }

    /**
     * Switches meshes and loads new bitmaps if available. Updated to support 2
     * pages in landscape
     */
    private fun startCurl(page: Int) {
        when (page) {

            // Once right side page is curled, first right page is assigned into
            // curled page. And if there are more bitmaps available new bitmap is
            // loaded into right side mesh.
            CURL_RIGHT -> {
                // Remove meshes from renderer.
                mRenderer.removeCurlMesh(mPageLeft)
                mRenderer.removeCurlMesh(mPageRight)
                mRenderer.removeCurlMesh(mPageCurl)

                // We are curling right page.
                val curl = mPageRight
                mPageRight = mPageCurl
                mPageCurl = curl

                if (mCurrentIndex > 0) {
                    mPageLeft.setFlipTexture(true)
                    mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
                    mPageLeft.reset()
                    if (mRenderLeftPage) {
                        mRenderer.addCurlMesh(mPageLeft)
                    }
                }
                if (mCurrentIndex < mPageProvider!!.pageCount - 1) {
                    updatePage(mPageRight.texturePage, mCurrentIndex + 1)
                    mPageRight.setRect(
                        mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!
                    )
                    mPageRight.setFlipTexture(false)
                    mPageRight.reset()
                    mRenderer.addCurlMesh(mPageRight)
                }

                // Add curled page to renderer.
                mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!)
                mPageCurl.setFlipTexture(false)
                mPageCurl.reset()
                mRenderer.addCurlMesh(mPageCurl)

                mCurlState = CURL_RIGHT
            }

            // On left side curl, left page is assigned to curled page. And if
            // there are more bitmaps available before currentIndex, new bitmap
            // is loaded into left page.
            CURL_LEFT -> {
                // Remove meshes from renderer.
                mRenderer.removeCurlMesh(mPageLeft)
                mRenderer.removeCurlMesh(mPageRight)
                mRenderer.removeCurlMesh(mPageCurl)

                // We are curling left page.
                val curl = mPageLeft
                mPageLeft = mPageCurl
                mPageCurl = curl

                if (mCurrentIndex > 1) {
                    updatePage(mPageLeft.texturePage, mCurrentIndex - 2)
                    mPageLeft.setFlipTexture(true)
                    mPageLeft
                        .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
                    mPageLeft.reset()
                    if (mRenderLeftPage) {
                        mRenderer.addCurlMesh(mPageLeft)
                    }
                }

                // If there is something to show on right page add it to renderer.
                if (mCurrentIndex < mPageProvider!!.pageCount) {
                    mPageRight.setFlipTexture(false)
                    mPageRight.setRect(
                        mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!
                    )
                    mPageRight.reset()
                    mRenderer.addCurlMesh(mPageRight)
                }

                // How dragging previous page happens depends on view mode.
                if (mViewMode == SHOW_ONE_PAGE || mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES) {
                    mPageCurl.setRect(
                        mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!
                    )
                    mPageCurl.setFlipTexture(false)
                } else {
                    mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
                    mPageCurl.setFlipTexture(true)
                }
                mPageCurl.reset()
                mRenderer.addCurlMesh(mPageCurl)

                mCurlState = CURL_LEFT
            }
        }
    }

    /**
     * Updates curl position.
     */
    private fun updateCurlPos(pointerPos: PointerPosition) {

        // Default curl radius.
        var radius = (mRenderer.getPageRect(CURL_RIGHT)!!.width() / 3).toDouble()
        // TODO: This is not an optimal solution. Based on feedback received so
        // far; pressure is not very accurate, it may be better not to map
        // coefficient to range [0f, 1f] but something like [.2f, 1f] instead.
        // Leaving it as is until get my hands on a real device. On emulator
        // this doesn't work anyway.
        radius *= max(1f - pointerPos.mPressure, 0f).toDouble()
        // NOTE: Here we set pointerPos to mCurlPos. It might be a bit confusing
        // later to see e.g "mCurlPos.x - mDragStartPos.x" used. But it's
        // actually pointerPos we are doing calculations against. Why? Simply to
        // optimize code a bit with the cost of making it unreadable. Otherwise
        // we had to this in both of the next if-else branches.
        mCurlPos.set(pointerPos.mPos)

        // If curl happens on right page, or on left page on two page mode,
        // we'll calculate curl position from pointerPos.
        if (mCurlState == CURL_RIGHT || mCurlState == CURL_LEFT && mViewMode == SHOW_TWO_PAGES) {

            mCurlDir.x = mCurlPos.x - mDragStartPos.x
            mCurlDir.y = mCurlPos.y - mDragStartPos.y
            val dist =
                sqrt((mCurlDir.x * mCurlDir.x + mCurlDir.y * mCurlDir.y).toDouble()).toFloat()

            // Adjust curl radius so that if page is dragged far enough on
            // opposite side, radius gets closer to zero.
            val pageWidth = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!
                .width()
            var curlLen = radius * Math.PI
            if (dist > pageWidth * 2 - curlLen) {
                curlLen = max(pageWidth * 2 - dist, 0f).toDouble()
                radius = curlLen / Math.PI
            }

            // Actual curl position calculation.
            if (dist >= curlLen) {
                val translate = (dist - curlLen) / 2
                if (mViewMode == SHOW_TWO_PAGES) {
                    mCurlPos.x -= (mCurlDir.x * translate / dist).toFloat()
                } else {
                    val pageLeftX = mRenderer
                        .getPageRect(CurlRenderer.PAGE_RIGHT)!!.left
                    radius = max(
                        min((mCurlPos.x - pageLeftX).toDouble(), radius),
                        0.0
                    )
                }
                mCurlPos.y -= (mCurlDir.y * translate / dist).toFloat()
            } else {
                val angle = Math.PI * sqrt(dist / curlLen)
                val translate = radius * sin(angle)
                mCurlPos.x += (mCurlDir.x * translate / dist).toFloat()
                mCurlPos.y += (mCurlDir.y * translate / dist).toFloat()
            }
        } else if (mCurlState == CURL_LEFT) {

            // Adjust radius regarding how close to page edge we are.
            val pageLeftX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!.left
            radius = max(min((mCurlPos.x - pageLeftX).toDouble(), radius), 0.0)

            val pageRightX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!.right
            mCurlPos.x -= min((pageRightX - mCurlPos.x).toDouble(), radius).toFloat()
            mCurlDir.x = mCurlPos.x + mDragStartPos.x
            mCurlDir.y = mCurlPos.y - mDragStartPos.y
        }// Otherwise we'll let curl follow pointer position.

        setCurlPos(mCurlPos, mCurlDir, radius)
    }

    /**
     * Updates given CurlPage via PageProvider for page located at index.
     */
    private fun updatePage(page: CurlPage, index: Int) {
        // First reset page to initial state.
        page.reset()
        // Ask page provider to fill it up with bitmaps and colors.
        mPageProvider!!.updatePage(
            page, mPageBitmapWidth, mPageBitmapHeight,
            index
        )
    }

    /**
     * Updates bitmaps for page meshes.
     */
    fun updatePages() {
        if (mPageProvider == null || mPageBitmapWidth <= 0
            || mPageBitmapHeight <= 0
        ) {
            return
        }

        // Remove meshes from renderer.
        mRenderer.removeCurlMesh(mPageLeft)
        mRenderer.removeCurlMesh(mPageRight)
        mRenderer.removeCurlMesh(mPageCurl)

        var leftIdx = mCurrentIndex - 1
        var rightIdx = mCurrentIndex
        var curlIdx = -1
        if (mCurlState == CURL_LEFT) {
            curlIdx = leftIdx
            --leftIdx
        } else if (mCurlState == CURL_RIGHT) {
            curlIdx = rightIdx
            ++rightIdx
        }

        if (rightIdx >= 0 && rightIdx < mPageProvider!!.pageCount) {
            updatePage(mPageRight.texturePage, rightIdx)
            mPageRight.setFlipTexture(false)
            mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!)
            mPageRight.reset()
            mRenderer.addCurlMesh(mPageRight)
        }
        if (leftIdx >= 0 && leftIdx < mPageProvider!!.pageCount) {
            updatePage(mPageLeft.texturePage, leftIdx)
            mPageLeft.setFlipTexture(true)
            mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
            mPageLeft.reset()
            if (mRenderLeftPage) {
                mRenderer.addCurlMesh(mPageLeft)
            }
        }
        if (curlIdx >= 0 && curlIdx < mPageProvider!!.pageCount) {
            updatePage(mPageCurl.texturePage, curlIdx)

            if (mCurlState == CURL_RIGHT) {
                mPageCurl.setFlipTexture(true)
                mPageCurl.setRect(
                    mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)!!
                )
            } else {
                mPageCurl.setFlipTexture(false)
                mPageCurl
                    .setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT)!!)
            }

            mPageCurl.reset()
            mRenderer.addCurlMesh(mPageCurl)
        }
    }

    /**
     * Provider for feeding 'book' with bitmaps which are used for rendering
     * pages.
     */
    interface PageProvider {

        /**
         * Return number of pages available.
         */
        val pageCount: Int

        /**
         * Called once new bitmaps/textures are needed. Width and height are in
         * pixels telling the size it will be drawn on screen and following them
         * ensures that aspect ratio remains. But it's possible to return bitmap
         * of any size though. You should use provided CurlPage for storing page
         * information for requested page number.<br></br>
         * <br></br>
         * Index is a number between 0 and getBitmapCount() - 1.
         */
        fun updatePage(page: CurlPage, width: Int, height: Int, index: Int)
    }

    /**
     * Simple holder for pointer position.
     */
    private inner class PointerPosition {
        internal var mPos = PointF()
        internal var mPressure: Float = 0.toFloat()
    }

    /**
     * Observer interface for handling CurlView size changes.
     */
    interface SizeChangedObserver {

        /**
         * Called once CurlView size changes.
         */
        fun onSizeChanged(width: Int, height: Int)
    }

    companion object {

        // Curl state. We are flipping none, left or right page.
        private const val CURL_LEFT = 1
        private const val CURL_NONE = 0
        private const val CURL_RIGHT = 2

        // Constants for mAnimationTargetEvent.
        private const val SET_CURL_TO_LEFT = 1
        private const val SET_CURL_TO_RIGHT = 2

        // Shows one page at the center of view.
        const val SHOW_ONE_PAGE = 1
        // Shows two pages side by side.
        const val SHOW_TWO_PAGES = 2
    }

}
