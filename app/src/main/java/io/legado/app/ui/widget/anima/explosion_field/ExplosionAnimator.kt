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

import android.animation.ValueAnimator
import android.graphics.*
import android.view.View
import android.view.animation.AccelerateInterpolator
import java.util.*
import kotlin.math.pow

class ExplosionAnimator(private val mContainer: View, bitmap: Bitmap, bound: Rect) :
    ValueAnimator() {
    private val mPaint: Paint = Paint()
    private val mParticles: Array<Particle?>
    private val mBound: Rect = Rect(bound)

    init {
        val partLen = 15
        mParticles = arrayOfNulls(partLen * partLen)
        val random = Random(System.currentTimeMillis())
        val w = bitmap.width / (partLen + 2)
        val h = bitmap.height / (partLen + 2)
        for (i in 0 until partLen) {
            for (j in 0 until partLen) {
                mParticles[i * partLen + j] =
                    generateParticle(bitmap.getPixel((j + 1) * w, (i + 1) * h), random)
            }
        }
        setFloatValues(0f, END_VALUE)
        interpolator = DEFAULT_INTERPOLATOR
        duration = DEFAULT_DURATION
    }

    private fun generateParticle(color: Int, random: Random): Particle {
        val particle = Particle()
        particle.color = color
        particle.radius = V
        if (random.nextFloat() < 0.2f) {
            particle.baseRadius = V + (X - V) * random.nextFloat()
        } else {
            particle.baseRadius = W + (V - W) * random.nextFloat()
        }
        val nextFloat = random.nextFloat()
        particle.top = mBound.height() * (0.18f * random.nextFloat() + 0.2f)
        particle.top =
            if (nextFloat < 0.2f) particle.top else particle.top + particle.top * 0.2f * random.nextFloat()
        particle.bottom = mBound.height() * (random.nextFloat() - 0.5f) * 1.8f
        var f =
            if (nextFloat < 0.2f) particle.bottom else if (nextFloat < 0.8f) particle.bottom * 0.6f else particle.bottom * 0.3f
        particle.bottom = f
        particle.mag = 4.0f * particle.top / particle.bottom
        particle.neg = -particle.mag / particle.bottom
        f = mBound.centerX() + Y * (random.nextFloat() - 0.5f)
        particle.baseCx = f
        particle.cx = f
        f = mBound.centerY() + Y * (random.nextFloat() - 0.5f)
        particle.baseCy = f
        particle.cy = f
        particle.life = END_VALUE / 10 * random.nextFloat()
        particle.overflow = 0.4f * random.nextFloat()
        particle.alpha = 1f
        return particle
    }

    fun draw(canvas: Canvas): Boolean {
        if (!isStarted) {
            return false
        }
        for (particle in mParticles) {
            particle?.let {
                particle.advance(animatedValue as Float)
                if (particle.alpha > 0f) {
                    mPaint.color = particle.color
                    mPaint.alpha = (Color.alpha(particle.color) * particle.alpha).toInt()
                    canvas.drawCircle(particle.cx, particle.cy, particle.radius, mPaint)
                }
            }
        }
        mContainer.invalidate()
        return true
    }

    override fun start() {
        super.start()
        mContainer.invalidate()
    }

    private inner class Particle {
        var alpha: Float = 0.toFloat()
        var color: Int = 0
        var cx: Float = 0.toFloat()
        var cy: Float = 0.toFloat()
        var radius: Float = 0.toFloat()
        var baseCx: Float = 0.toFloat()
        var baseCy: Float = 0.toFloat()
        var baseRadius: Float = 0.toFloat()
        var top: Float = 0.toFloat()
        var bottom: Float = 0.toFloat()
        var mag: Float = 0.toFloat()
        var neg: Float = 0.toFloat()
        var life: Float = 0.toFloat()
        var overflow: Float = 0.toFloat()


        fun advance(factor: Float) {
            var f = 0f
            var normalization = factor / END_VALUE
            if (normalization < life || normalization > 1f - overflow) {
                alpha = 0f
                return
            }
            normalization = (normalization - life) / (1f - life - overflow)
            val f2 = normalization * END_VALUE
            if (normalization >= 0.7f) {
                f = (normalization - 0.7f) / 0.3f
            }
            alpha = 1f - f
            f = bottom * f2
            cx = baseCx + f
            cy = (baseCy - this.neg * f.toDouble().pow(2.0)).toFloat() - f * mag
            radius = V + (baseRadius - V) * f2
        }
    }

    companion object {

        internal var DEFAULT_DURATION: Long = 0x400
        private val DEFAULT_INTERPOLATOR = AccelerateInterpolator(0.6f)
        private const val END_VALUE = 1.4f
        private val X = Utils.dp2Px(5).toFloat()
        private val Y = Utils.dp2Px(20).toFloat()
        private val V = Utils.dp2Px(2).toFloat()
        private val W = Utils.dp2Px(1).toFloat()
    }
}
