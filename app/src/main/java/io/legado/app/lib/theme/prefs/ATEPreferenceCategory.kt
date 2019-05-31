package io.legado.app.lib.theme.prefs

import android.content.Context
import android.os.Build
import android.preference.PreferenceCategory
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import io.legado.app.lib.theme.ThemeStore


class ATEPreferenceCategory : PreferenceCategory {

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
        if (view is TextView) {
            view.setTextColor(ThemeStore.accentColor(view.getContext()))//设置title文本的颜色
        }
    }

}
