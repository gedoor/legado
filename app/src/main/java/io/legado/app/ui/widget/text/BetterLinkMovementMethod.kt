package io.legado.app.ui.widget.text

import android.app.Activity
import android.graphics.RectF
import android.text.Selection
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.*
import android.widget.TextView
import io.legado.app.R
import io.legado.app.ui.widget.text.BetterLinkMovementMethod.LongPressTimer.OnTimerReachedListener

class BetterLinkMovementMethod protected constructor() : LinkMovementMethod() {
    private var onLinkClickListener: OnLinkClickListener? = null
    private var onLinkLongClickListener: OnLinkLongClickListener? = null
    private val touchedLineBounds = RectF()
    private var isUrlHighlighted = false
    private var clickableSpanUnderTouchOnActionDown: ClickableSpan? = null
    private var activeTextViewHashcode = 0
    private var ongoingLongPressTimer: LongPressTimer? = null
    private var wasLongPressRegistered = false

    interface OnLinkClickListener {
        /**
         * @param textView The TextView on which a click was registered.
         * @param url      The clicked URL.
         * @return True if this click was handled. False to let Android handle the URL.
         */
        fun onClick(textView: TextView?, url: String?): Boolean
    }

    interface OnLinkLongClickListener {
        /**
         * @param textView The TextView on which a long-click was registered.
         * @param url      The long-clicked URL.
         * @return True if this long-click was handled. False to let Android handle the URL (as a short-click).
         */
        fun onLongClick(textView: TextView?, url: String?): Boolean
    }

    /**
     * Set a listener that will get called whenever any link is clicked on the TextView.
     */
    fun setOnLinkClickListener(clickListener: OnLinkClickListener?): BetterLinkMovementMethod {
        if (this === singleInstance) {
            throw UnsupportedOperationException(
                "Setting a click listener on the instance returned by getInstance() is not supported to avoid memory " +
                        "leaks. Please use newInstance() or any of the linkify() methods instead."
            )
        }
        onLinkClickListener = clickListener
        return this
    }

    /**
     * Set a listener that will get called whenever any link is clicked on the TextView.
     */
    fun setOnLinkLongClickListener(longClickListener: OnLinkLongClickListener?): BetterLinkMovementMethod {
        if (this === singleInstance) {
            throw UnsupportedOperationException(
                "Setting a long-click listener on the instance returned by getInstance() is not supported to avoid " +
                        "memory leaks. Please use newInstance() or any of the linkify() methods instead."
            )
        }
        onLinkLongClickListener = longClickListener
        return this
    }

    override fun onTouchEvent(textView: TextView, text: Spannable, event: MotionEvent): Boolean {
        if (activeTextViewHashcode != textView.hashCode()) {
            // Bug workaround: TextView stops calling onTouchEvent() once any URL is highlighted.
            // A hacky solution is to reset any "autoLink" property set in XML. But we also want
            // to do this once per TextView.
            activeTextViewHashcode = textView.hashCode()
            textView.autoLinkMask = 0
        }
        val clickableSpanUnderTouch = findClickableSpanUnderTouch(textView, text, event)
        if (event.action == MotionEvent.ACTION_DOWN) {
            clickableSpanUnderTouchOnActionDown = clickableSpanUnderTouch
        }
        val touchStartedOverAClickableSpan = clickableSpanUnderTouchOnActionDown != null
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clickableSpanUnderTouch?.let { highlightUrl(textView, it, text) }
                if (touchStartedOverAClickableSpan && onLinkLongClickListener != null) {
                    val longClickListener: OnTimerReachedListener =
                        object : OnTimerReachedListener {
                            override fun onTimerReached() {
                                wasLongPressRegistered = true
                                textView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                removeUrlHighlightColor(textView)
                                dispatchUrlLongClick(textView, clickableSpanUnderTouch)
                            }
                        }
                    startTimerForRegisteringLongClick(textView, longClickListener)
                }
                touchStartedOverAClickableSpan
            }
            MotionEvent.ACTION_UP -> {
                // Register a click only if the touch started and ended on the same URL.
                if (!wasLongPressRegistered && touchStartedOverAClickableSpan && clickableSpanUnderTouch === clickableSpanUnderTouchOnActionDown) {
                    dispatchUrlClick(textView, clickableSpanUnderTouch)
                }
                cleanupOnTouchUp(textView)

                // Consume this event even if we could not find any spans to avoid letting Android handle this event.
                // Android's TextView implementation has a bug where links get clicked even when there is no more text
                // next to the link and the touch lies outside its bounds in the same direction.
                touchStartedOverAClickableSpan
            }
            MotionEvent.ACTION_CANCEL -> {
                cleanupOnTouchUp(textView)
                false
            }
            MotionEvent.ACTION_MOVE -> {
                // Stop listening for a long-press as soon as the user wanders off to unknown lands.
                if (clickableSpanUnderTouch !== clickableSpanUnderTouchOnActionDown) {
                    removeLongPressCallback(textView)
                }
                if (!wasLongPressRegistered) {
                    // Toggle highlight.
                    if (clickableSpanUnderTouch != null) {
                        highlightUrl(textView, clickableSpanUnderTouch, text)
                    } else {
                        removeUrlHighlightColor(textView)
                    }
                }
                touchStartedOverAClickableSpan
            }
            else -> false
        }
    }

    private fun cleanupOnTouchUp(textView: TextView) {
        wasLongPressRegistered = false
        clickableSpanUnderTouchOnActionDown = null
        removeUrlHighlightColor(textView)
        removeLongPressCallback(textView)
    }

    /**
     * Determines the touched location inside the TextView's text and returns the ClickableSpan found under it (if any).
     *
     * @return The touched ClickableSpan or null.
     */
    protected fun findClickableSpanUnderTouch(
        textView: TextView,
        text: Spannable,
        event: MotionEvent
    ): ClickableSpan? {
        // So we need to find the location in text where touch was made, regardless of whether the TextView
        // has scrollable text. That is, not the entire text is currently visible.
        var touchX = event.x.toInt()
        var touchY = event.y.toInt()

        // Ignore padding.
        touchX -= textView.totalPaddingLeft
        touchY -= textView.totalPaddingTop

        // Account for scrollable text.
        touchX += textView.scrollX
        touchY += textView.scrollY
        val layout = textView.layout
        val touchedLine = layout.getLineForVertical(touchY)
        val touchOffset = layout.getOffsetForHorizontal(touchedLine, touchX.toFloat())
        touchedLineBounds.left = layout.getLineLeft(touchedLine)
        touchedLineBounds.top = layout.getLineTop(touchedLine).toFloat()
        touchedLineBounds.right = layout.getLineWidth(touchedLine) + touchedLineBounds.left
        touchedLineBounds.bottom = layout.getLineBottom(touchedLine).toFloat()
        return if (touchedLineBounds.contains(touchX.toFloat(), touchY.toFloat())) {
            // Find a ClickableSpan that lies under the touched area.
            val spans = text.getSpans(touchOffset, touchOffset, ClickableSpan::class.java)
            for (span in spans) {
                if (span is ClickableSpan) {
                    return span
                }
            }
            // No ClickableSpan found under the touched location.
            null
        } else {
            // Touch lies outside the line's horizontal bounds where no spans should exist.
            null
        }
    }

    /**
     * Adds a background color span at <var>clickableSpan</var>'s location.
     */
    protected fun highlightUrl(textView: TextView, clickableSpan: ClickableSpan?, text: Spannable) {
        if (isUrlHighlighted) {
            return
        }
        isUrlHighlighted = true
        val spanStart = text.getSpanStart(clickableSpan)
        val spanEnd = text.getSpanEnd(clickableSpan)
        val highlightSpan = BackgroundColorSpan(textView.highlightColor)
        text.setSpan(highlightSpan, spanStart, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        textView.setTag(R.id.bettermovementmethod_highlight_background_span, highlightSpan)
        Selection.setSelection(text, spanStart, spanEnd)
    }

    /**
     * Removes the highlight color under the Url.
     */
    protected fun removeUrlHighlightColor(textView: TextView) {
        if (!isUrlHighlighted) {
            return
        }
        isUrlHighlighted = false
        val text = textView.text as Spannable
        val highlightSpan =
            textView.getTag(R.id.bettermovementmethod_highlight_background_span) as BackgroundColorSpan
        text.removeSpan(highlightSpan)
        Selection.removeSelection(text)
    }

    protected fun startTimerForRegisteringLongClick(
        textView: TextView,
        longClickListener: OnTimerReachedListener?
    ) {
        ongoingLongPressTimer = LongPressTimer()
        ongoingLongPressTimer!!.setOnTimerReachedListener(longClickListener)
        textView.postDelayed(
            ongoingLongPressTimer,
            ViewConfiguration.getLongPressTimeout().toLong()
        )
    }

    /**
     * Remove the long-press detection timer.
     */
    protected fun removeLongPressCallback(textView: TextView) {
        if (ongoingLongPressTimer != null) {
            textView.removeCallbacks(ongoingLongPressTimer)
            ongoingLongPressTimer = null
        }
    }

    protected fun dispatchUrlClick(textView: TextView, clickableSpan: ClickableSpan?) {
        val clickableSpanWithText = ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkClickListener != null && onLinkClickListener!!.onClick(
            textView,
            clickableSpanWithText.text()
        )
        if (!handled) {
            // Let Android handle this click.
            clickableSpanWithText.span()!!.onClick(textView)
        }
    }

    protected fun dispatchUrlLongClick(textView: TextView, clickableSpan: ClickableSpan?) {
        val clickableSpanWithText = ClickableSpanWithText.ofSpan(textView, clickableSpan)
        val handled = onLinkLongClickListener != null && onLinkLongClickListener!!.onLongClick(
            textView,
            clickableSpanWithText.text()
        )
        if (!handled) {
            // Let Android handle this long click as a short-click.
            clickableSpanWithText.span()!!.onClick(textView)
        }
    }

    protected class LongPressTimer : Runnable {
        private var onTimerReachedListener: OnTimerReachedListener? = null

        interface OnTimerReachedListener {
            fun onTimerReached()
        }

        override fun run() {
            onTimerReachedListener!!.onTimerReached()
        }

        fun setOnTimerReachedListener(listener: OnTimerReachedListener?) {
            onTimerReachedListener = listener
        }
    }

    /**
     * A wrapper to support all [ClickableSpan]s that may or may not provide URLs.
     */
    protected class ClickableSpanWithText protected constructor(
        private val span: ClickableSpan?,
        private val text: String
    ) {
        fun span(): ClickableSpan? {
            return span
        }

        fun text(): String {
            return text
        }

        companion object {
            fun ofSpan(textView: TextView, span: ClickableSpan?): ClickableSpanWithText {
                val s = textView.text as Spanned
                val text: String
                text = if (span is URLSpan) {
                    span.url
                } else {
                    val start = s.getSpanStart(span)
                    val end = s.getSpanEnd(span)
                    s.subSequence(start, end).toString()
                }
                return ClickableSpanWithText(span, text)
            }
        }
    }

    companion object {
        private var singleInstance: BetterLinkMovementMethod? = null
        private const val LINKIFY_NONE = -2

        /**
         * Return a new instance of BetterLinkMovementMethod.
         */
        fun newInstance(): BetterLinkMovementMethod {
            return BetterLinkMovementMethod()
        }

        /**
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @param textViews   The TextViews on which a [BetterLinkMovementMethod] should be registered.
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, vararg textViews: TextView): BetterLinkMovementMethod {
            val movementMethod = newInstance()
            for (textView in textViews) {
                addLinks(linkifyMask, movementMethod, textView)
            }
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @param textViews The TextViews on which a [BetterLinkMovementMethod] should be registered.
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkifyHtml(vararg textViews: TextView): BetterLinkMovementMethod {
            return linkify(LINKIFY_NONE, *textViews)
        }

        /**
         * Recursively register a [BetterLinkMovementMethod] on every TextView inside a layout.
         *
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, viewGroup: ViewGroup): BetterLinkMovementMethod {
            val movementMethod = newInstance()
            rAddLinks(linkifyMask, viewGroup, movementMethod)
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkifyHtml(viewGroup: ViewGroup): BetterLinkMovementMethod {
            return linkify(LINKIFY_NONE, viewGroup)
        }

        /**
         * Recursively register a [BetterLinkMovementMethod] on every TextView inside a layout.
         *
         * @param linkifyMask One of [Linkify.ALL], [Linkify.PHONE_NUMBERS], [Linkify.MAP_ADDRESSES],
         * [Linkify.WEB_URLS] and [Linkify.EMAIL_ADDRESSES].
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkify(linkifyMask: Int, activity: Activity): BetterLinkMovementMethod {
            // Find the layout passed to setContentView().
            val activityLayout =
                (activity.findViewById<View>(Window.ID_ANDROID_CONTENT) as ViewGroup).getChildAt(0) as ViewGroup
            val movementMethod = newInstance()
            rAddLinks(linkifyMask, activityLayout, movementMethod)
            return movementMethod
        }

        /**
         * Like [.linkify], but can be used for TextViews with HTML links.
         *
         * @return The registered [BetterLinkMovementMethod] on the TextViews.
         */
        fun linkifyHtml(activity: Activity): BetterLinkMovementMethod {
            return linkify(LINKIFY_NONE, activity)
        }

        /**
         * Get a static instance of BetterLinkMovementMethod. Do note that registering a click listener on the returned
         * instance is not supported because it will potentially be shared on multiple TextViews.
         */
        val instance: BetterLinkMovementMethod?
            get() {
                if (singleInstance == null) {
                    singleInstance = BetterLinkMovementMethod()
                }
                return singleInstance
            }

        // ======== PUBLIC APIs END ======== //
        private fun rAddLinks(
            linkifyMask: Int,
            viewGroup: ViewGroup,
            movementMethod: BetterLinkMovementMethod
        ) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (child is ViewGroup) {
                    // Recursively find child TextViews.
                    rAddLinks(linkifyMask, child, movementMethod)
                } else if (child is TextView) {
                    addLinks(linkifyMask, movementMethod, child)
                }
            }
        }

        private fun addLinks(
            linkifyMask: Int,
            movementMethod: BetterLinkMovementMethod,
            textView: TextView
        ) {
            textView.movementMethod = movementMethod
            if (linkifyMask != LINKIFY_NONE) {
                Linkify.addLinks(textView, linkifyMask)
            }
        }
    }
}