@file:Suppress("NOTHING_TO_INLINE", "unused")

package io.legado.app.lib.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.fragment.app.Fragment

inline fun <D : DialogInterface> Fragment.selector(
    noinline factory: AlertBuilderFactory<D>,
    title: CharSequence? = null,
    items: List<CharSequence>,
    noinline onClick: (DialogInterface, CharSequence, Int) -> Unit
) = requireActivity().selector(factory, title, items, onClick)

fun <D : DialogInterface> Context.selector(
    factory: AlertBuilderFactory<D>,
    title: CharSequence? = null,
    items: List<CharSequence>,
    onClick: (DialogInterface, CharSequence, Int) -> Unit
) {
    with(factory(this)) {
        if (title != null) {
            this.title = title
        }
        items(items, onClick)
        show()
    }
}
