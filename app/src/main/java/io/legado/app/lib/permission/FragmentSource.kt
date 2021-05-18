package io.legado.app.lib.permission

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment

import java.lang.ref.WeakReference

internal class FragmentSource(fragment: Fragment) : RequestSource {

    private val fragRef: WeakReference<Fragment> = WeakReference(fragment)

    override val context: Context?
        get() = fragRef.get()?.requireContext()

    override fun startActivity(intent: Intent) {
        fragRef.get()?.startActivity(intent)
    }
}
