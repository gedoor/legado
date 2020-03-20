package io.legado.app.utils

import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import io.legado.app.R

@Suppress("unused")
object UIUtils {

    /** 设置更多工具条图标和颜色  */
    fun setToolbarMoreIconCustomColor(toolbar: Toolbar, colorId: Int? = null) {
        if (toolbar == null)
            return
        val moreIcon = ContextCompat.getDrawable(toolbar.context, R.drawable.ic_more)
        if(moreIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (colorId != null ) {
                moreIcon.colorFilter = PorterDuffColorFilter(ContextCompat.getColor(toolbar.context, colorId), PorterDuff.Mode.SRC_ATOP)
            }
            toolbar.overflowIcon = moreIcon
        }
    }

}