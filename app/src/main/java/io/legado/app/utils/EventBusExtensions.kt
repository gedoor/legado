@file:Suppress("unused")

package io.legado.app.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.core.Observable

inline fun <reified EVENT> eventObservable(tag: String): Observable<EVENT> {
    return LiveEventBus.get(tag, EVENT::class.java)
}

inline fun <reified EVENT> postEvent(tag: String, event: EVENT) {
    LiveEventBus.get(tag).post(event)
}

inline fun <reified EVENT> postEventDelay(tag: String, event: EVENT, delay: Long) {
    LiveEventBus.get(tag).postDelay(event, delay)
}

inline fun <reified EVENT> AppCompatActivity.observeEvent(
    vararg tags: String,
    noinline observer: (EVENT) -> Unit
) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observe(this, o)
    }
}


inline fun <reified EVENT> AppCompatActivity.observeEventSticky(
    vararg tags: String,
    noinline observer: (EVENT) -> Unit
) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observeSticky(this, o)
    }
}

inline fun <reified EVENT> Fragment.observeEvent(
    vararg tags: String,
    noinline observer: (EVENT) -> Unit
) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observe(this, o)
    }
}

inline fun <reified EVENT> Fragment.observeEventSticky(
    vararg tags: String,
    noinline observer: (EVENT) -> Unit
) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observeSticky(this, o)
    }
}

