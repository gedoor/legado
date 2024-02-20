package io.legado.app.utils.canvasrecorder

import android.graphics.Canvas
import java.util.concurrent.locks.ReentrantLock

class CanvasRecorderLocked(private val delegate: CanvasRecorder) :
    CanvasRecorder by delegate {

    var lock: ReentrantLock? = ReentrantLock()

    private fun initLock() {
        if (lock == null) {
            synchronized(this) {
                if (lock == null) {
                    lock = ReentrantLock()
                }
            }
        }
    }

    override fun beginRecording(width: Int, height: Int): Canvas {
        initLock()
        lock!!.lock()
        return delegate.beginRecording(width, height)
    }

    override fun endRecording() {
        delegate.endRecording()
        lock!!.unlock()
    }

    override fun draw(canvas: Canvas) {
        if (lock == null) {
            return
        }
        lock!!.lock()
        try {
            delegate.draw(canvas)
        } finally {
            lock!!.unlock()
        }
    }

    override fun isLocked(): Boolean {
        if (lock == null) {
            return false
        }
        return lock!!.isLocked
    }

    override fun recycle() {
        if (lock == null) {
            return
        }
        lock!!.lock()
        try {
            delegate.recycle()
        } finally {
            lock!!.unlock()
        }
        lock = null
    }

}
