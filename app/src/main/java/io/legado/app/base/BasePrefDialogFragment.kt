package io.legado.app.base

import android.view.Gravity
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.dpToPx


abstract class BasePrefDialogFragment(
) : DialogFragment() {

    override fun onStart() {
        super.onStart()
        if (AppConfig.isEInkMode) {
            dialog?.window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                val attr = it.attributes
                attr.dimAmount = 0.0f
                attr.windowAnimations = 0
                it.attributes = attr
                it.setBackgroundDrawableResource(R.color.transparent)
            }

            // 修改gravity的时机一般在子类的onStart方法中, 因此需要在onStart之后执行.
            this.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    if (event == Lifecycle.Event.ON_START) {
                        when (dialog?.window?.attributes?.gravity) {
                            Gravity.TOP -> view?.setBackgroundResource(R.drawable.bg_eink_border_bottom)
                            Gravity.BOTTOM -> view?.setBackgroundResource(R.drawable.bg_eink_border_top)
                            else -> {
                                val padding = 2.dpToPx();
                                view?.setPadding(padding, padding, padding, padding)
                                view?.setBackgroundResource(R.drawable.bg_eink_border_dialog)
                            }
                        }
                    }
                }
            })
        }
    }
}