package io.legado.app.ui.widget.recycler

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.help.AppConfig
import kotlinx.android.synthetic.main.view_refresh_recycler.view.*


class RefreshRecyclerView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var durTouchX = -1000000f
    private var durTouchY = -1000000f

    var onRefreshStart: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_refresh_recycler, this, true)
        recycler_view.isEnableScroll = !AppConfig.isEInkMode
        recycler_view.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        durTouchX = event.x
                        durTouchY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (durTouchX == -1000000f) {
                            durTouchX = event.x
                        }
                        if (durTouchY == -1000000f)
                            durTouchY = event.y

                        val dY = event.y - durTouchY  //>0下拉
                        durTouchY = event.y
                        if (!refresh_progress_bar.isAutoLoading && refresh_progress_bar.getSecondDurProgress() == refresh_progress_bar.secondFinalProgress) {
                            recycler_view.adapter?.let {
                                if (it.itemCount > 0) {
                                    if (0 == (recycler_view.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()) {
                                        refresh_progress_bar.setSecondDurProgress((refresh_progress_bar.getSecondDurProgress() + dY / 2).toInt())
                                    }
                                } else {
                                    refresh_progress_bar.setSecondDurProgress((refresh_progress_bar.getSecondDurProgress() + dY / 2).toInt())
                                }
                            }
                            return refresh_progress_bar.getSecondDurProgress() > 0
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!refresh_progress_bar.isAutoLoading && refresh_progress_bar.secondMaxProgress > 0 && refresh_progress_bar.getSecondDurProgress() > 0) {
                            if (refresh_progress_bar.getSecondDurProgress() >= refresh_progress_bar.secondMaxProgress) {
                                refresh_progress_bar.isAutoLoading = true
                                onRefreshStart?.invoke()
                            } else {
                                refresh_progress_bar.setSecondDurProgressWithAnim(0)
                            }
                        }
                        durTouchX = -1000000f
                        durTouchY = -1000000f
                    }
                }
                return false
            }
        })
    }

    fun startLoading() {
        refresh_progress_bar.isAutoLoading = true
        onRefreshStart?.invoke()
    }

    fun stopLoading() {
        refresh_progress_bar.isAutoLoading = false
    }
}