package io.legado.app.ui.widget.page.delegate

import android.view.MotionEvent

fun MotionEvent.toAction(action: Int): MotionEvent {
    return MotionEvent.obtain(
        downTime,
        eventTime,
        action,
        x,
        y,
        pressure,
        size,
        metaState,
        xPrecision,
        yPrecision,
        deviceId,
        edgeFlags
    )
}