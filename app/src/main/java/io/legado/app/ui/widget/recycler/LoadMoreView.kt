package io.legado.app.ui.widget.recycler

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import io.legado.app.R
import io.legado.app.utils.invisible
import io.legado.app.utils.visible
import kotlinx.android.synthetic.main.view_load_more.view.*

@Suppress("unused")
class LoadMoreView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    var hasMore = true
        private set

    init {
        View.inflate(context, R.layout.view_load_more, this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
    }

    fun startLoad() {
        tv_text.invisible()
        rotate_loading.show()
    }

    fun stopLoad() {
        rotate_loading.hide()
    }
    
    fun hasMore() {
        hasMore = true
        tv_text.invisible()
        rotate_loading.show()
    }
    
    fun noMore(msg: String? = null) {
        hasMore = false
        rotate_loading.hide()
        if (msg != null) {
            tv_text.text = msg
        } else {
            tv_text.setText(R.string.bottom_line)
        }
        tv_text.visible()
    }

}
