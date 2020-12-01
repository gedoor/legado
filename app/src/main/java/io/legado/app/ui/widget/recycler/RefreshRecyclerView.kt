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
import io.legado.app.databinding.ViewRefreshRecyclerBinding
import io.legado.app.lib.theme.ATH


@SuppressLint("ClickableViewAccessibility")
class RefreshRecyclerView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var binding: ViewRefreshRecyclerBinding
    private var durTouchX = -1000000f
    private var durTouchY = -1000000f

    var onRefreshStart: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        val view = LayoutInflater.from(context).inflate(R.layout.view_refresh_recycler, this, true)
        binding = ViewRefreshRecyclerBinding.bind(view)
        ATH.applyEdgeEffectColor(binding.recyclerView)
        binding.recyclerView.setOnTouchListener(object : OnTouchListener {
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
                        if (!binding.refreshProgressBar.isAutoLoading &&
                            binding.refreshProgressBar.getSecondDurProgress() == binding.refreshProgressBar.secondFinalProgress
                        ) {
                            binding.recyclerView.adapter?.let {
                                if (it.itemCount > 0) {
                                    if (0 == (binding.recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()) {
                                        binding.refreshProgressBar.setSecondDurProgress((binding.refreshProgressBar.getSecondDurProgress() + dY / 2).toInt())
                                    }
                                } else {
                                    binding.refreshProgressBar.setSecondDurProgress((binding.refreshProgressBar.getSecondDurProgress() + dY / 2).toInt())
                                }
                            }
                            return binding.refreshProgressBar.getSecondDurProgress() > 0
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!binding.refreshProgressBar.isAutoLoading &&
                            binding.refreshProgressBar.secondMaxProgress > 0 &&
                            binding.refreshProgressBar.getSecondDurProgress() > 0
                        ) {
                            if (binding.refreshProgressBar.getSecondDurProgress() >= binding.refreshProgressBar.secondMaxProgress) {
                                binding.refreshProgressBar.isAutoLoading = true
                                onRefreshStart?.invoke()
                            } else {
                                binding.refreshProgressBar.setSecondDurProgressWithAnim(0)
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

    val recyclerView get() = binding.recyclerView

    fun startLoading() {
        binding.refreshProgressBar.isAutoLoading = true
        onRefreshStart?.invoke()
    }

    fun stopLoading() {
        binding.refreshProgressBar.isAutoLoading = false
    }
}