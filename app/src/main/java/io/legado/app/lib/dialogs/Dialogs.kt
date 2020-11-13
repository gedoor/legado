@file:Suppress("NOTHING_TO_INLINE", "unused")

package io.legado.app.lib.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.fragment.app.Fragment

typealias AlertBuilderFactory<D> = (Context) -> AlertBuilder<D>

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    title: String? = null,
    message: String? = null,
    noinline init: (AlertBuilder<D>.() -> Unit)? = null
) = activity?.alert(factory, title, message, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    title: String? = null,
    message: String? = null,
    init: (AlertBuilder<D>.() -> Unit)? = null
): AlertBuilder<D> {
    return factory(this).apply {
        if (title != null) {
            this.title = title
        }
        if (message != null) {
            this.message = message
        }
        if (init != null) init()
    }
}

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    titleResource: Int? = null,
    messageResource: Int? = null,
    noinline init: (AlertBuilder<D>.() -> Unit)? = null
) = requireActivity().alert(factory, titleResource, messageResource, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    titleResource: Int? = null,
    messageResource: Int? = null,
    init: (AlertBuilder<D>.() -> Unit)? = null
): AlertBuilder<D> {
    return factory(this).apply {
        if (titleResource != null) {
            this.titleResource = titleResource
        }
        if (messageResource != null) {
            this.messageResource = messageResource
        }
        if (init != null) init()
    }
}

inline fun <D : DialogInterface> Fragment.alert(
    noinline factory: AlertBuilderFactory<D>,
    noinline init: AlertBuilder<D>.() -> Unit
) = requireActivity().alert(factory, init)

fun <D : DialogInterface> Context.alert(
    factory: AlertBuilderFactory<D>,
    init: AlertBuilder<D>.() -> Unit
): AlertBuilder<D> = factory(this).apply { init() }
