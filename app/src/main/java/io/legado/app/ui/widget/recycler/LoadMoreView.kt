package io.legado.app.ui.widget.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.databinding.ViewLoadMoreBinding
import io.legado.app.utils.invisible
import io.legado.app.utils.visible

@Suppress("unused")
class LoadMoreView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewLoadMoreBinding.inflate(LayoutInflater.from(context), this)
    var hasMore = true
        private set

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
    }

    fun startLoad() {
        binding.tvText.invisible()
        binding.rotateLoading.show()
    }

    fun stopLoad() {
        binding.rotateLoading.hide()
    }
    
    fun hasMore() {
        hasMore = true
        binding.tvText.invisible()
        binding.rotateLoading.show()
    }

    fun noMore(msg: String? = null) {
        hasMore = false
        binding.rotateLoading.hide()
        if (msg != null) {
            binding.tvText.text = msg
        } else {
            binding.tvText.setText(R.string.bottom_line)
        }
        binding.tvText.visible()
    }

    fun error(msg: String) {
        hasMore = false
        binding.rotateLoading.hide()
        binding.tvText.text = msg
        binding.tvText.visible()
    }

}
