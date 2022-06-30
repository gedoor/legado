package io.legado.app.lib.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.*
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.legado.app.R
import io.legado.app.utils.ColorUtils

/**
 * @author afollestad, plusCubed
 */
@Suppress("MemberVisibilityCanBePrivate")
object TintHelper {

    @SuppressLint("PrivateResource")
    @ColorInt
    private fun getDefaultRippleColor(context: Context, useDarkRipple: Boolean): Int {
        // Light ripple is actually translucent black, and vice versa
        return ContextCompat.getColor(
            context, if (useDarkRipple)
                androidx.appcompat.R.color.ripple_material_light
            else
                androidx.appcompat.R.color.ripple_material_dark
        )
    }

    private fun getDisabledColorStateList(
        @ColorInt normal: Int,
        @ColorInt disabled: Int
    ): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_enabled)
            ), intArrayOf(disabled, normal)
        )
    }

    fun setTintSelector(view: View, @ColorInt color: Int, darker: Boolean, useDarkTheme: Boolean) {
        val isColorLight = ColorUtils.isColorLight(color)
        val disabled = ContextCompat.getColor(
            view.context,
            if (useDarkTheme) R.color.ate_button_disabled_dark else R.color.ate_button_disabled_light
        )
        val pressed = ColorUtils.shiftColor(color, if (darker) 0.9f else 1.1f)
        val activated = ColorUtils.shiftColor(color, if (darker) 1.1f else 0.9f)
        val rippleColor = getDefaultRippleColor(view.context, isColorLight)
        val textColor = ContextCompat.getColor(
            view.context,
            if (isColorLight) R.color.ate_primary_text_light else R.color.ate_primary_text_dark
        )

        val sl: ColorStateList
        when (view) {
            is Button -> {
                sl = getDisabledColorStateList(color, disabled)
                if (view.getBackground() is RippleDrawable) {
                    val rd = view.getBackground() as RippleDrawable
                    rd.setColor(ColorStateList.valueOf(rippleColor))
                }
                // Disabled text color state for buttons, may get overridden later by ATE tags
                view.setTextColor(
                    getDisabledColorStateList(
                        textColor,
                        ContextCompat.getColor(
                            view.getContext(),
                            if (useDarkTheme) R.color.ate_button_text_disabled_dark else R.color.ate_button_text_disabled_light
                        )
                    )
                )
            }
            is FloatingActionButton -> {
                // FloatingActionButton doesn't support disabled state?
                sl = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_pressed),
                        intArrayOf(android.R.attr.state_pressed)
                    ), intArrayOf(color, pressed)
                )

                view.rippleColor = rippleColor
                view.backgroundTintList = sl
                if (view.drawable != null)
                    view.setImageDrawable(createTintedDrawable(view.drawable, textColor))
                return
            }
            else -> {
                sl = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(android.R.attr.state_enabled),
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed),
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_activated),
                        intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
                    ),
                    intArrayOf(disabled, color, pressed, activated, activated)
                )
            }
        }

        var drawable: Drawable? = view.background
        if (drawable != null) {
            drawable = createTintedDrawable(drawable, sl)
            ViewUtils.setBackgroundCompat(view, drawable)
        }

        if (view is TextView && view !is Button) {
            view.setTextColor(
                getDisabledColorStateList(
                    textColor,
                    ContextCompat.getColor(
                        view.getContext(),
                        if (isColorLight) R.color.ate_text_disabled_light else R.color.ate_text_disabled_dark
                    )
                )
            )
        }
    }

    fun setTintAuto(
        view: View,
        @ColorInt color: Int,
        isBackground: Boolean,
        isDark: Boolean
    ) {
        var isBg = isBackground
        if (!isBg) {
            when (view) {
                is RadioButton -> setTint(view, color, isDark)
                is SeekBar -> setTint(view, color, isDark)
                is ProgressBar -> setTint(view, color)
                is AppCompatEditText -> setTint(view, color, isDark)
                is CheckBox -> setTint(view, color, isDark)
                is ImageView -> setTint(view, color)
                is Switch -> setTint(view, color, isDark)
                is SwitchCompat -> setTint(view, color, isDark)
                is SearchView -> {
                    val iconIdS =
                        intArrayOf(
                            androidx.appcompat.R.id.search_button,
                            androidx.appcompat.R.id.search_close_btn,
                            androidx.appcompat.R.id.search_go_btn,
                            androidx.appcompat.R.id.search_voice_btn,
                            androidx.appcompat.R.id.search_mag_icon
                        )
                    for (iconId in iconIdS) {
                        val icon = view.findViewById<ImageView>(iconId)
                        if (icon != null) {
                            setTint(icon, color)
                        }
                    }
                }
                else -> isBg = true
            }
            if (!isBg && view.background is RippleDrawable) {
                // Ripples for the above views (e.g. when you tap and hold a switch or checkbox)
                val rd = view.background as RippleDrawable
                @SuppressLint("PrivateResource") val unchecked = ContextCompat.getColor(
                    view.context,
                    if (isDark) androidx.appcompat.R.color.ripple_material_dark else androidx.appcompat.R.color.ripple_material_light
                )
                val checked = ColorUtils.adjustAlpha(color, 0.4f)
                val sl = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_activated, -android.R.attr.state_checked),
                        intArrayOf(android.R.attr.state_activated),
                        intArrayOf(android.R.attr.state_checked)
                    ),
                    intArrayOf(unchecked, checked, checked)
                )
                rd.setColor(sl)
            }
        }
        if (isBg) {
            // Need to tint the isBackground of a view
            if (view is FloatingActionButton || view is Button) {
                setTintSelector(view, color, false, isDark)
            } else if (view.background != null) {
                var drawable: Drawable? = view.background
                if (drawable != null) {
                    drawable = createTintedDrawable(drawable, color)
                    ViewUtils.setBackgroundCompat(view, drawable)
                }
            }
        }
    }

    @SuppressLint("PrivateResource")
    fun setTint(radioButton: RadioButton, @ColorInt color: Int, useDarker: Boolean) {
        val sl = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
            ), intArrayOf(
                // Radio button includes own alpha for disabled state
                ColorUtils.stripAlpha(
                    ContextCompat.getColor(
                        radioButton.context,
                        if (useDarker) R.color.ate_control_disabled_dark else R.color.ate_control_disabled_light
                    )
                ),
                ContextCompat.getColor(
                    radioButton.context,
                    if (useDarker) R.color.ate_control_normal_dark else R.color.ate_control_normal_light
                ),
                color
            )
        )
        radioButton.buttonTintList = sl
    }

    fun setTint(seekBar: SeekBar, @ColorInt color: Int, useDarker: Boolean) {
        val s1 = getDisabledColorStateList(
            color,
            ContextCompat.getColor(
                seekBar.context,
                if (useDarker) R.color.ate_control_disabled_dark else R.color.ate_control_disabled_light
            )
        )
        seekBar.thumbTintList = s1
        seekBar.progressTintList = s1
    }

    @JvmOverloads
    fun setTint(
        progressBar: ProgressBar, @ColorInt color: Int,
        skipIndeterminate: Boolean = false
    ) {
        val sl = ColorStateList.valueOf(color)
        progressBar.progressTintList = sl
        progressBar.secondaryProgressTintList = sl
        if (!skipIndeterminate)
            progressBar.indeterminateTintList = sl
    }


    @SuppressLint("RestrictedApi")
    fun setTint(editText: AppCompatEditText, @ColorInt color: Int, useDarker: Boolean) {
        val editTextColorStateList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(
                    android.R.attr.state_enabled,
                    -android.R.attr.state_pressed,
                    -android.R.attr.state_focused
                ),
                intArrayOf()
            ),
            intArrayOf(
                ContextCompat.getColor(
                    editText.context,
                    if (useDarker) R.color.ate_text_disabled_dark else R.color.ate_text_disabled_light
                ),
                ContextCompat.getColor(
                    editText.context,
                    if (useDarker) R.color.ate_control_normal_dark else R.color.ate_control_normal_light
                ),
                color
            )
        )
        editText.supportBackgroundTintList = editTextColorStateList
        setCursorTint(editText, color)
    }

    @SuppressLint("PrivateResource")
    fun setTint(box: CheckBox, @ColorInt color: Int, useDarker: Boolean) {
        val sl = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
            ),
            intArrayOf(
                ContextCompat.getColor(
                    box.context,
                    if (useDarker) R.color.ate_control_disabled_dark else R.color.ate_control_disabled_light
                ),
                ContextCompat.getColor(
                    box.context,
                    if (useDarker) R.color.ate_control_normal_dark else R.color.ate_control_normal_light
                ),
                color
            )
        )
        box.buttonTintList = sl
    }

    fun setTint(image: ImageView, @ColorInt color: Int) {
        image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    private fun modifySwitchDrawable(
        context: Context,
        from: Drawable,
        @ColorInt tint: Int,
        thumb: Boolean,
        compatSwitch: Boolean,
        useDarker: Boolean
    ): Drawable? {
        var tint1 = tint
        if (useDarker) {
            tint1 = ColorUtils.shiftColor(tint1, 1.1f)
        }
        tint1 = ColorUtils.adjustAlpha(tint1, if (compatSwitch && !thumb) 0.5f else 1.0f)
        val disabled: Int
        var normal: Int
        if (thumb) {
            disabled = ContextCompat.getColor(
                context,
                if (useDarker) R.color.ate_switch_thumb_disabled_dark else R.color.ate_switch_thumb_disabled_light
            )
            normal = ContextCompat.getColor(
                context,
                if (useDarker) R.color.ate_switch_thumb_normal_dark else R.color.ate_switch_thumb_normal_light
            )
        } else {
            disabled = ContextCompat.getColor(
                context,
                if (useDarker) R.color.ate_switch_track_disabled_dark else R.color.ate_switch_track_disabled_light
            )
            normal = ContextCompat.getColor(
                context,
                if (useDarker) R.color.ate_switch_track_normal_dark else R.color.ate_switch_track_normal_light
            )
        }

        // Stock switch includes its own alpha
        if (!compatSwitch) {
            normal = ColorUtils.stripAlpha(normal)
        }

        val sl = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(
                    android.R.attr.state_enabled,
                    -android.R.attr.state_activated,
                    -android.R.attr.state_checked
                ),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked)
            ),
            intArrayOf(disabled, normal, tint1, tint1)
        )
        return createTintedDrawable(from, sl)
    }

    fun setTint(
        @SuppressLint("UseSwitchCompatOrMaterialCode") switchView: Switch,
        @ColorInt color: Int,
        useDarker: Boolean
    ) {
        if (switchView.trackDrawable != null) {
            switchView.trackDrawable = modifySwitchDrawable(
                switchView.context,
                switchView.trackDrawable,
                color,
                thumb = false,
                compatSwitch = false,
                useDarker = useDarker
            )
        }
        if (switchView.thumbDrawable != null) {
            switchView.thumbDrawable = modifySwitchDrawable(
                switchView.context,
                switchView.thumbDrawable,
                color,
                thumb = true,
                compatSwitch = false,
                useDarker = useDarker
            )
        }
    }

    fun setTint(switchView: SwitchCompat, @ColorInt color: Int, useDarker: Boolean) {
        if (switchView.trackDrawable != null) {
            switchView.trackDrawable = modifySwitchDrawable(
                switchView.context,
                switchView.trackDrawable,
                color,
                thumb = false,
                compatSwitch = true,
                useDarker = useDarker
            )
        }
        if (switchView.thumbDrawable != null) {
            switchView.thumbDrawable = modifySwitchDrawable(
                switchView.context,
                switchView.thumbDrawable,
                color,
                thumb = true,
                compatSwitch = true,
                useDarker = useDarker
            )
        }
    }

    // This returns a NEW Drawable because of the mutate() call. The mutate() call is necessary because Drawables with the same resource have shared states otherwise.
    @CheckResult
    fun createTintedDrawable(drawable: Drawable?, @ColorInt color: Int): Drawable? {
        var drawable1: Drawable? = drawable ?: return null
        drawable1 = DrawableCompat.wrap(drawable1!!.mutate())
        DrawableCompat.setTintMode(drawable1, PorterDuff.Mode.SRC_IN)
        DrawableCompat.setTint(drawable1, color)
        return drawable1
    }

    // This returns a NEW Drawable because of the mutate() call. The mutate() call is necessary because Drawables with the same resource have shared states otherwise.
    @CheckResult
    fun createTintedDrawable(drawable: Drawable?, sl: ColorStateList): Drawable? {
        var drawable1: Drawable? = drawable ?: return null
        drawable1 = DrawableCompat.wrap(drawable1!!.mutate())
        DrawableCompat.setTintList(drawable1, sl)
        return drawable1
    }

    @SuppressLint("DiscouragedPrivateApi", "SoonBlockedPrivateApi")
    fun setCursorTint(editText: EditText, @ColorInt color: Int) {
        try {
            val fCursorDrawableRes = TextView::class.java.getDeclaredField("mCursorDrawableRes")
            fCursorDrawableRes.isAccessible = true
            val mCursorDrawableRes = fCursorDrawableRes.getInt(editText)
            val fEditor = TextView::class.java.getDeclaredField("mEditor")
            fEditor.isAccessible = true
            val editor = fEditor.get(editText)
            val clazz = editor.javaClass
            val fCursorDrawable = clazz.getDeclaredField("mCursorDrawable")
            fCursorDrawable.isAccessible = true
            val drawables = arrayOfNulls<Drawable>(2)
            drawables[0] = ContextCompat.getDrawable(editText.context, mCursorDrawableRes)
            drawables[0] = createTintedDrawable(drawables[0], color)
            drawables[1] = ContextCompat.getDrawable(editText.context, mCursorDrawableRes)
            drawables[1] = createTintedDrawable(drawables[1], color)
            fCursorDrawable.set(editor, drawables)
        } catch (ignored: Exception) {
        }

    }
}