@file:Suppress("RedundantVisibilityModifier", "unused")

package io.legado.app.utils.viewbindingdelegate

import android.view.LayoutInflater
import androidx.core.app.ComponentActivity
import androidx.viewbinding.ViewBinding

/**
 * Create new [ViewBinding] associated with the [ComponentActivity]
 */
@JvmName("viewBindingActivity")
inline fun <T : ViewBinding> ComponentActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T,
    setContentView: Boolean = false
) = lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val binding = bindingInflater.invoke(layoutInflater)
    if (setContentView) {
        setContentView(binding.root)
    }
    binding
}
