package io.legado.app.ui.widget.prefs

import android.content.Context
import android.content.ContextWrapper
import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.jaredrummler.android.colorpicker.*
import io.legado.app.lib.theme.ATH
import io.legado.app.utils.ColorUtils

@Suppress("MemberVisibilityCanBePrivate", "unused")
class ColorPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs),
    ColorPickerDialogListener {

    var onSaveColor: ((color: Int) -> Boolean)? = null

    private val sizeNormal = 0
    private val sizeLarge = 1

    private var onShowDialogListener: OnShowDialogListener? = null
    private var mColor = Color.BLACK
    private var showDialog: Boolean = false

    @ColorPickerDialog.DialogType
    private var dialogType: Int = 0
    private var colorShape: Int = 0
    private var allowPresets: Boolean = false
    private var allowCustom: Boolean = false
    private var showAlphaSlider: Boolean = false
    private var showColorShades: Boolean = false
    private var previewSize: Int = 0
    private var presets: IntArray? = null
    private var dialogTitle: Int = 0

    init {
        isPersistent = true
        layoutResource = io.legado.app.R.layout.view_preference

        val a = context.obtainStyledAttributes(attrs, R.styleable.ColorPreference)
        showDialog = a.getBoolean(R.styleable.ColorPreference_cpv_showDialog, true)

        dialogType =
            a.getInt(R.styleable.ColorPreference_cpv_dialogType, ColorPickerDialog.TYPE_PRESETS)
        colorShape = a.getInt(R.styleable.ColorPreference_cpv_colorShape, ColorShape.CIRCLE)
        allowPresets = a.getBoolean(R.styleable.ColorPreference_cpv_allowPresets, true)
        allowCustom = a.getBoolean(R.styleable.ColorPreference_cpv_allowCustom, true)
        showAlphaSlider = a.getBoolean(R.styleable.ColorPreference_cpv_showAlphaSlider, false)
        showColorShades = a.getBoolean(R.styleable.ColorPreference_cpv_showColorShades, true)
        previewSize = a.getInt(R.styleable.ColorPreference_cpv_previewSize, sizeNormal)
        val presetsResId = a.getResourceId(R.styleable.ColorPreference_cpv_colorPresets, 0)
        dialogTitle =
            a.getResourceId(R.styleable.ColorPreference_cpv_dialogTitle, R.string.cpv_default_title)
        presets = if (presetsResId != 0) {
            context.resources.getIntArray(presetsResId)
        } else {
            ColorPickerDialog.MATERIAL_COLORS
        }
        widgetLayoutResource = if (colorShape == ColorShape.CIRCLE) {
            if (previewSize == sizeLarge) R.layout.cpv_preference_circle_large else R.layout.cpv_preference_circle
        } else {
            if (previewSize == sizeLarge) R.layout.cpv_preference_square_large else R.layout.cpv_preference_square
        }
        a.recycle()
    }

    override fun onClick() {
        super.onClick()
        if (onShowDialogListener != null) {
            onShowDialogListener!!.onShowColorPickerDialog(title as String, mColor)
        } else if (showDialog) {
            val dialog = ColorPickerDialogCompat.newBuilder()
                .setDialogType(dialogType)
                .setDialogTitle(dialogTitle)
                .setColorShape(colorShape)
                .setPresets(presets!!)
                .setAllowPresets(allowPresets)
                .setAllowCustom(allowCustom)
                .setShowAlphaSlider(showAlphaSlider)
                .setShowColorShades(showColorShades)
                .setColor(mColor)
                .create()
            dialog.setColorPickerDialogListener(this)
            getActivity().supportFragmentManager
                .beginTransaction()
                .add(dialog, getFragmentTag())
                .commitAllowingStateLoss()
        }
    }

    private fun getActivity(): FragmentActivity {
        val context = context
        if (context is FragmentActivity) {
            return context
        } else if (context is ContextWrapper) {
            val baseContext = context.baseContext
            if (baseContext is FragmentActivity) {
                return baseContext
            }
        }
        throw IllegalStateException("Error getting activity from context")
    }

    override fun onAttached() {
        super.onAttached()
        if (showDialog) {
            val fragment =
                getActivity().supportFragmentManager.findFragmentByTag(getFragmentTag()) as ColorPickerDialog?
            fragment?.setColorPickerDialogListener(this)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        val v = io.legado.app.ui.widget.prefs.Preference.bindView<ColorPanelView>(
            context, holder, icon, title, summary, widgetLayoutResource,
            io.legado.app.R.id.cpv_preference_preview_color_panel, 30, 30
        )
        if (v is ColorPanelView) {
            v.color = mColor
        }
        super.onBindViewHolder(holder)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        if (defaultValue is Int) {
            mColor = if (!showAlphaSlider) ColorUtils.withAlpha(defaultValue, 1f) else defaultValue
            persistInt(mColor)
        } else {
            mColor = getPersistedInt(-0x1000000)
        }
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        return a!!.getInteger(index, Color.BLACK)
    }

    override fun onColorSelected(dialogId: Int, @ColorInt color: Int) {
        //返回值为true时说明已经处理过,不再处理
        if (onSaveColor?.invoke(color) == true) {
            return
        }
        saveValue(color)
    }

    override fun onDialogDismissed(dialogId: Int) {
        // no-op
    }

    /**
     * Set the new color
     *
     * @param color The newly selected color
     */
    fun saveValue(@ColorInt color: Int) {
        mColor = if (showAlphaSlider) color else ColorUtils.withAlpha(color, 1f)
        persistInt(mColor)
        notifyChanged()
        callChangeListener(color)
    }

    /**
     * Get the colors that will be shown in the [ColorPickerDialog].
     *
     * @return An array of color ints
     */
    fun getPresets(): IntArray? {
        return presets
    }

    /**
     * Set the colors shown in the [ColorPickerDialog].
     *
     * @param presets An array of color ints
     */
    fun setPresets(presets: IntArray) {
        this.presets = presets
    }

    /**
     * The listener used for showing the [ColorPickerDialog].
     * Call [.saveValue] after the user chooses a color.
     * If this is set then it is up to you to show the dialog.
     *
     * @param listener The listener to show the dialog
     */
    fun setOnShowDialogListener(listener: OnShowDialogListener) {
        onShowDialogListener = listener
    }

    /**
     * The tag used for the [ColorPickerDialog].
     *
     * @return The tag
     */
    fun getFragmentTag(): String {
        return "color_$key"
    }

    interface OnShowDialogListener {

        fun onShowColorPickerDialog(title: String, currentColor: Int)
    }


    internal class ColorPickerDialogCompat : ColorPickerDialog() {

        override fun onStart() {
            super.onStart()
            val alertDialog = dialog as? AlertDialog
            alertDialog?.let {
                ATH.setAlertDialogTint(it)
            }
        }


        companion object {
            fun newBuilder(): Builder {
                return Builder()
            }

            private const val ARG_ID = "id"
            private const val ARG_TYPE = "dialogType"
            private const val ARG_COLOR = "color"
            private const val ARG_ALPHA = "alpha"
            private const val ARG_PRESETS = "presets"
            private const val ARG_ALLOW_PRESETS = "allowPresets"
            private const val ARG_ALLOW_CUSTOM = "allowCustom"
            private const val ARG_DIALOG_TITLE = "dialogTitle"
            private const val ARG_SHOW_COLOR_SHADES = "showColorShades"
            private const val ARG_COLOR_SHAPE = "colorShape"
            private const val ARG_PRESETS_BUTTON_TEXT = "presetsButtonText"
            private const val ARG_CUSTOM_BUTTON_TEXT = "customButtonText"
            private const val ARG_SELECTED_BUTTON_TEXT = "selectedButtonText"
        }

        class Builder internal constructor() {

            internal var colorPickerDialogListener: ColorPickerDialogListener? = null

            @StringRes
            internal var dialogTitle = R.string.cpv_default_title

            @StringRes
            internal var presetsButtonText = R.string.cpv_presets

            @StringRes
            internal var customButtonText = R.string.cpv_custom

            @StringRes
            internal var selectedButtonText = R.string.cpv_select

            @DialogType
            internal var dialogType = TYPE_PRESETS
            internal var presets = MATERIAL_COLORS

            @ColorInt
            internal var color = Color.BLACK
            internal var dialogId = 0
            internal var showAlphaSlider = false
            internal var allowPresets = true
            internal var allowCustom = true
            internal var showColorShades = true

            @ColorShape
            internal var colorShape = ColorShape.CIRCLE

            /**
             * Set the dialog title string resource id
             *
             * @param dialogTitle The string resource used for the dialog title
             * @return This builder object for chaining method calls
             */
            fun setDialogTitle(@StringRes dialogTitle: Int): Builder {
                this.dialogTitle = dialogTitle
                return this
            }

            /**
             * Set the selected button text string resource id
             *
             * @param selectedButtonText The string resource used for the selected button text
             * @return This builder object for chaining method calls
             */
            fun setSelectedButtonText(@StringRes selectedButtonText: Int): Builder {
                this.selectedButtonText = selectedButtonText
                return this
            }

            /**
             * Set the presets button text string resource id
             *
             * @param presetsButtonText The string resource used for the presets button text
             * @return This builder object for chaining method calls
             */
            fun setPresetsButtonText(@StringRes presetsButtonText: Int): Builder {
                this.presetsButtonText = presetsButtonText
                return this
            }

            /**
             * Set the custom button text string resource id
             *
             * @param customButtonText The string resource used for the custom button text
             * @return This builder object for chaining method calls
             */
            fun setCustomButtonText(@StringRes customButtonText: Int): Builder {
                this.customButtonText = customButtonText
                return this
            }

            /**
             * Set which dialog view to show.
             *
             * @param dialogType Either [ColorPickerDialog.TYPE_CUSTOM] or [ColorPickerDialog.TYPE_PRESETS].
             * @return This builder object for chaining method calls
             */
            fun setDialogType(@DialogType dialogType: Int): Builder {
                this.dialogType = dialogType
                return this
            }

            /**
             * Set the colors used for the presets
             *
             * @param presets An array of color ints.
             * @return This builder object for chaining method calls
             */
            fun setPresets(presets: IntArray): Builder {
                this.presets = presets
                return this
            }

            /**
             * Set the original color
             *
             * @param color The default color for the color picker
             * @return This builder object for chaining method calls
             */
            fun setColor(color: Int): Builder {
                this.color = color
                return this
            }

            /**
             * Set the dialog id used for callbacks
             *
             * @param dialogId The id that is sent back to the [ColorPickerDialogListener].
             * @return This builder object for chaining method calls
             */
            fun setDialogId(dialogId: Int): Builder {
                this.dialogId = dialogId
                return this
            }

            /**
             * Show the alpha slider
             *
             * @param showAlphaSlider `true` to show the alpha slider. Currently only supported with the [ ].
             * @return This builder object for chaining method calls
             */
            fun setShowAlphaSlider(showAlphaSlider: Boolean): Builder {
                this.showAlphaSlider = showAlphaSlider
                return this
            }

            /**
             * Show/Hide a neutral button to select preset colors.
             *
             * @param allowPresets `false` to disable showing the presets button.
             * @return This builder object for chaining method calls
             */
            fun setAllowPresets(allowPresets: Boolean): Builder {
                this.allowPresets = allowPresets
                return this
            }

            /**
             * Show/Hide the neutral button to select a custom color.
             *
             * @param allowCustom `false` to disable showing the custom button.
             * @return This builder object for chaining method calls
             */
            fun setAllowCustom(allowCustom: Boolean): Builder {
                this.allowCustom = allowCustom
                return this
            }

            /**
             * Show/Hide the color shades in the presets picker
             *
             * @param showColorShades `false` to hide the color shades.
             * @return This builder object for chaining method calls
             */
            fun setShowColorShades(showColorShades: Boolean): Builder {
                this.showColorShades = showColorShades
                return this
            }

            /**
             * Set the shape of the color panel view.
             *
             * @param colorShape Either [ColorShape.CIRCLE] or [ColorShape.SQUARE].
             * @return This builder object for chaining method calls
             */
            fun setColorShape(colorShape: Int): Builder {
                this.colorShape = colorShape
                return this
            }

            /**
             * Create the [ColorPickerDialog] instance.
             *
             * @return A new [ColorPickerDialog].
             * @see .show
             */
            fun create(): ColorPickerDialog {
                val dialog =
                    ColorPickerDialogCompat()
                val args = Bundle()
                args.putInt(ARG_ID, dialogId)
                args.putInt(ARG_TYPE, dialogType)
                args.putInt(ARG_COLOR, color)
                args.putIntArray(ARG_PRESETS, presets)
                args.putBoolean(ARG_ALPHA, showAlphaSlider)
                args.putBoolean(ARG_ALLOW_CUSTOM, allowCustom)
                args.putBoolean(ARG_ALLOW_PRESETS, allowPresets)
                args.putInt(ARG_DIALOG_TITLE, dialogTitle)
                args.putBoolean(ARG_SHOW_COLOR_SHADES, showColorShades)
                args.putInt(ARG_COLOR_SHAPE, colorShape)
                args.putInt(ARG_PRESETS_BUTTON_TEXT, presetsButtonText)
                args.putInt(ARG_CUSTOM_BUTTON_TEXT, customButtonText)
                args.putInt(ARG_SELECTED_BUTTON_TEXT, selectedButtonText)
                dialog.arguments = args
                return dialog
            }

        }

    }
}
