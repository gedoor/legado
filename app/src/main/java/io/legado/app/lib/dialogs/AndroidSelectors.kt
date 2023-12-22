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

@file:Suppress("unused")

package io.legado.app.lib.dialogs

import android.content.Context
import android.content.DialogInterface

fun Context.selector(
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        items(items, onClick)
        show()
    }
}

fun Context.selector(
    title: CharSequence,
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(title)
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    title: CharSequence,
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(title)
        items(items, onClick)
        show()
    }
}

fun Context.selector(
    titleSource: Int,
    items: List<CharSequence>,
    onClick: (DialogInterface, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(titleSource)
        items(items, onClick)
        show()
    }
}

fun <T> Context.selector(
    titleSource: Int,
    items: List<T>,
    onClick: (DialogInterface, T, Int) -> Unit
) {
    with(AndroidAlertBuilder(this)) {
        this.setTitle(titleSource)
        items(items, onClick)
        show()
    }
}
