@file:Suppress("NOTHING_TO_INLINE", "unused")

package io.legado.app.lib.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.legado.app.R
import org.jetbrains.anko.internals.AnkoInternals.NO_GETTER
import kotlin.DeprecationLevel.ERROR

@SuppressLint("SupportAnnotationUsage")
interface AlertBuilder<out D : DialogInterface> {
    val ctx: Context

    var title: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get

    var titleResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var message: CharSequence
        @Deprecated(NO_GETTER, level = ERROR) get

    var messageResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var icon: Drawable
        @Deprecated(NO_GETTER, level = ERROR) get

    @setparam:DrawableRes
    var iconResource: Int
        @Deprecated(NO_GETTER, level = ERROR) get

    var customTitle: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var customView: View
        @Deprecated(NO_GETTER, level = ERROR) get

    var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = ERROR) get

    fun positiveButton(buttonText: String, onClicked: ((dialog: DialogInterface) -> Unit)? = null)
    fun positiveButton(
        @StringRes buttonTextResource: Int,
        onClicked: ((dialog: DialogInterface) -> Unit)? = null
    )

    fun negativeButton(buttonText: String, onClicked: ((dialog: DialogInterface) -> Unit)? = null)
    fun negativeButton(
        @StringRes buttonTextResource: Int,
        onClicked: ((dialog: DialogInterface) -> Unit)? = null
    )

    fun neutralButton(buttonText: String, onClicked: ((dialog: DialogInterface) -> Unit)? = null)
    fun neutralButton(
        @StringRes buttonTextResource: Int,
        onClicked: ((dialog: DialogInterface) -> Unit)? = null
    )

    fun onCancelled(handler: (dialog: DialogInterface) -> Unit)

    fun onKeyPressed(handler: (dialog: DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean)

    fun onDismiss(handler: (dialog: DialogInterface) -> Unit)

    fun items(
        items: List<CharSequence>,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun <T> items(
        items: List<T>,
        onItemSelected: (dialog: DialogInterface, item: T, index: Int) -> Unit
    )

    fun multiChoiceItems(
        items: Array<String>,
        checkedItems: BooleanArray,
        onClick: (dialog: DialogInterface, which: Int, isChecked: Boolean) -> Unit
    )

    fun singleChoiceItems(
        items: Array<String>,
        checkedItem: Int = 0,
        onClick: ((dialog: DialogInterface, which: Int) -> Unit)? = null
    )

    fun build(): D
    fun show(): D


    fun customTitle(view: () -> View) {
        customTitle = view()
    }

    fun customView(view: () -> View) {
        customView = view()
    }

    fun okButton(handler: ((dialog: DialogInterface) -> Unit)? = null) =
        positiveButton(android.R.string.ok, handler)

    fun cancelButton(handler: ((dialog: DialogInterface) -> Unit)? = null) =
        negativeButton(android.R.string.cancel, handler)

    fun yesButton(handler: ((dialog: DialogInterface) -> Unit)? = null) =
        positiveButton(R.string.yes, handler)

    fun noButton(handler: ((dialog: DialogInterface) -> Unit)? = null) =
        negativeButton(R.string.no, handler)
}
