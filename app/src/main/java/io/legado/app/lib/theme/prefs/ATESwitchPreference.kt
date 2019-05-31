package io.legado.app.lib.theme.prefs

import android.content.Context
import android.os.Build
import android.preference.SwitchPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.annotation.RequiresApi
import io.legado.app.lib.theme.ATH
import io.legado.app.lib.theme.ThemeStore
import java.util.*

class ATESwitchPreference : SwitchPreference {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    )

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onBindView(view: View) {
        super.onBindView(view)

        if (view is ViewGroup) {
            val queue = LinkedList<ViewGroup>()
            queue.add(view)
            while (!queue.isEmpty()) {
                val current = queue.removeFirst()
                for (i in 0 until current.childCount) {
                    if (current.getChildAt(i) is Switch) {
                        ATH.setTint(current.getChildAt(i), ThemeStore.accentColor(view.getContext()))
                        return
                    } else if (current.getChildAt(i) is ViewGroup) {
                        queue.addLast(current.getChildAt(i) as ViewGroup)
                    }
                }
            }
        }

    }

}
