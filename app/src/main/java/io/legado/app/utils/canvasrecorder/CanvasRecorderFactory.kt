package io.legado.app.utils.canvasrecorder

import android.os.Build
import io.legado.app.help.config.AppConfig

object CanvasRecorderFactory {

    private val atLeastApi23 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private val atLeastApi29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun create(locked: Boolean = false): CanvasRecorder {
        val impl = when {
            !AppConfig.optimizeRender -> CanvasRecorderImpl()
            atLeastApi29 -> CanvasRecorderApi29Impl()
            atLeastApi23 -> CanvasRecorderApi23Impl()
            else -> CanvasRecorderImpl()
        }
        return if (locked) {
            CanvasRecorderLocked(impl)
        } else {
            impl
        }
    }

}
