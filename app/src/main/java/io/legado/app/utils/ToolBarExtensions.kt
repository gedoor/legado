package io.legado.app.utils

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import io.legado.app.R

/**
 * 设置toolBar更多图标颜色
 */
fun Toolbar.setMoreIconColor(color: Int) {
    val moreIcon = ContextCompat.getDrawable(context, R.drawable.ic_more)
    if (moreIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        moreIcon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        overflowIcon = moreIcon
    }
}