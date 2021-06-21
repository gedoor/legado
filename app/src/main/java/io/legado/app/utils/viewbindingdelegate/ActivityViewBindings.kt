@file:Suppress("RedundantVisibilityModifier", "unused")

package io.legado.app.utils.viewbindingdelegate

import android.view.LayoutInflater
import androidx.core.app.ComponentActivity
import androidx.viewbinding.ViewBinding

inline fun <T : ViewBinding> ComponentActivity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val invoke = bindingInflater.invoke(layoutInflater)
        invoke
    }
