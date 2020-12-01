@file:Suppress("RedundantVisibilityModifier", "unused")
@file:JvmName("ReflectionFragmentViewBindings")

package io.legado.app.utils.viewbindingdelegate

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

private class FragmentViewBindingProperty<F : Fragment, T : ViewBinding>(
    viewBinder: (F) -> T
) : ViewBindingProperty<F, T>(viewBinder) {

    override fun getLifecycleOwner(thisRef: F) = thisRef.viewLifecycleOwner
}

/**
 * Create new [ViewBinding] associated with the [Fragment]
 */
@JvmName("viewBindingFragment")
public fun <F : Fragment, T : ViewBinding> Fragment.viewBinding(viewBinder: (F) -> T): ViewBindingProperty<F, T> {
    return FragmentViewBindingProperty(viewBinder)
}

/**
 * Create new [ViewBinding] associated with the [Fragment]
 *
 * @param vbFactory Function that create new instance of [ViewBinding]. `MyViewBinding::bind` can be used
 * @param viewProvider Provide a [View] from the Fragment. By default call [Fragment.requireView]
 */
@JvmName("viewBindingFragment")
public inline fun <F : Fragment, T : ViewBinding> Fragment.viewBinding(
    crossinline vbFactory: (View) -> T,
    crossinline viewProvider: (F) -> View = Fragment::requireView
): ViewBindingProperty<F, T> {
    return viewBinding { fragment: F -> vbFactory(viewProvider(fragment)) }
}

/**
 * Create new [ViewBinding] associated with the [Fragment]
 *
 * @param vbFactory Function that create new instance of [ViewBinding]. `MyViewBinding::bind` can be used
 * @param viewBindingRootId Root view's id that will be used as root for the view binding
 */
@JvmName("viewBindingFragment")
public inline fun <T : ViewBinding> Fragment.viewBinding(
    crossinline vbFactory: (View) -> T,
    @IdRes viewBindingRootId: Int
): ViewBindingProperty<Fragment, T> {
    return viewBinding(vbFactory) { fragment: Fragment ->
        fragment.requireView().findViewById(viewBindingRootId)
    }
}