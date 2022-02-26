/*
 * Copyright (C) 2015 tyrantgit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.legado.app.ui.widget.anima.explosion_field


import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import io.legado.app.utils.printOnDebug


import kotlin.math.roundToInt

object Utils {

    private val DENSITY = Resources.getSystem().displayMetrics.density
    private val sCanvas = Canvas()

    fun dp2Px(dp: Int): Int {
        return (dp * DENSITY).roundToInt()
    }

    fun createBitmapFromView(view: View): Bitmap? {
        if (view is ImageView) {
            val drawable = view.drawable
            if (drawable != null && drawable is BitmapDrawable) {
                return drawable.bitmap
            }
        }
        view.clearFocus()
        val bitmap = createBitmapSafely(
            view.width,
            view.height, Bitmap.Config.ARGB_8888, 1
        )
        if (bitmap != null) {
            synchronized(sCanvas) {
                val canvas = sCanvas
                canvas.setBitmap(bitmap)
                view.draw(canvas)
                canvas.setBitmap(null)
            }
        }
        return bitmap
    }

    private fun createBitmapSafely(
        width: Int,
        height: Int,
        config: Bitmap.Config,
        retryCount: Int
    ): Bitmap? {
        try {
            return Bitmap.createBitmap(width, height, config)
        } catch (e: OutOfMemoryError) {
            e.printOnDebug()
            if (retryCount > 0) {
                System.gc()
                return createBitmapSafely(width, height, config, retryCount - 1)
            }
            return null
        }

    }
}
