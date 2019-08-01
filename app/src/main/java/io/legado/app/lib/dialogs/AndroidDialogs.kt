/*
 * Copyright 2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE", "unused")

package io.legado.app.lib.dialogs

import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import org.jetbrains.anko.AnkoContext

inline fun AnkoContext<*>.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = ctx.alert(title, message, init)

inline fun Fragment.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(title, message, init)

fun Context.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
): AlertBuilder<AlertDialog> {
    return AndroidAlertBuilder(this).apply {
        if (title != null) {
            this.title = title
        }
        if (message != null) {
            this.message = message
        }
        if (init != null) init()
    }
}

inline fun AnkoContext<*>.alert(
    titleResource: Int? = null,
    messageResource: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = ctx.alert(titleResource, messageResource, init)

inline fun Fragment.alert(
    title: Int? = null,
    message: Int? = null,
    noinline init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
) = requireActivity().alert(title, message, init)

fun Context.alert(
    titleResource: Int? = null,
    messageResource: Int? = null,
    init: (AlertBuilder<DialogInterface>.() -> Unit)? = null
): AlertBuilder<DialogInterface> {
    return AndroidAlertBuilder(this).apply {
        if (titleResource != null) {
            this.titleResource = titleResource
        }
        if (messageResource != null) {
            this.messageResource = messageResource
        }
        if (init != null) init()
    }
}


inline fun AnkoContext<*>.alert(noinline init: AlertBuilder<DialogInterface>.() -> Unit) = ctx.alert(init)
inline fun Fragment.alert(noinline init: AlertBuilder<DialogInterface>.() -> Unit) = activity?.alert(init)

fun Context.alert(init: AlertBuilder<DialogInterface>.() -> Unit): AlertBuilder<DialogInterface> =
    AndroidAlertBuilder(this).apply { init() }

inline fun AnkoContext<*>.progressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.progressDialog(title, message, init)

inline fun Fragment.progressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().progressDialog(title, message, init)

fun Context.progressDialog(
    title: Int? = null,
    message: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title?.let { getString(it) }, message?.let { getString(it) }, false, init)


inline fun AnkoContext<*>.indeterminateProgressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.indeterminateProgressDialog(title, message, init)

inline fun Fragment.indeterminateProgressDialog(
    title: Int? = null,
    message: Int? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().indeterminateProgressDialog(title, message, init)

fun Context.indeterminateProgressDialog(
    title: Int? = null,
    message: Int? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title?.let { getString(it) }, message?.let { getString(it) }, true, init)


inline fun AnkoContext<*>.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.progressDialog(title, message, init)

inline fun Fragment.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().progressDialog(title, message, init)

fun Context.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title, message, false, init)


inline fun AnkoContext<*>.indeterminateProgressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = ctx.indeterminateProgressDialog(title, message, init)

inline fun Fragment.indeterminateProgressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    noinline init: (ProgressDialog.() -> Unit)? = null
) = requireActivity().indeterminateProgressDialog(title, message, init)

fun Context.indeterminateProgressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    init: (ProgressDialog.() -> Unit)? = null
) = progressDialog(title, message, true, init)


private fun Context.progressDialog(
    title: CharSequence? = null,
    message: CharSequence? = null,
    indeterminate: Boolean,
    init: (ProgressDialog.() -> Unit)? = null
) = ProgressDialog(this).apply {
    isIndeterminate = indeterminate
    if (!indeterminate) setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
    if (message != null) setMessage(message)
    if (title != null) setTitle(title)
    if (init != null) init()
    show()
}
