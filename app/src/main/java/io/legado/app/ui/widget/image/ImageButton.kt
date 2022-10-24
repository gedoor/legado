package io.legado.app.ui.widget.image

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class ImageButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {


    override fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            imageAlpha = if (enabled) 0xFF else 0x3F
        }
        super.setEnabled(enabled)
    }

}