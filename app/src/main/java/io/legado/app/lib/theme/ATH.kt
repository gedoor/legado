package io.legado.app.lib.theme

import android.graphics.drawable.GradientDrawable
import io.legado.app.utils.dp
import splitties.init.appCtx

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object ATH {

    fun getDialogBackground(): GradientDrawable {
        val background = GradientDrawable()
        background.cornerRadius = 3F.dp
        background.setColor(appCtx.backgroundColor)
        return background
    }

}