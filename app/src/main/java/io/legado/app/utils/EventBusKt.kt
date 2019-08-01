package io.legado.app.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus

inline fun <reified EVENT> eventObservable(tag: String): LiveEventBus.Observable<EVENT> {
    return LiveEventBus.get().with(tag, EVENT::class.java)
}

inline fun <reified EVENT> postEvent(tag: String, event: EVENT) {
    LiveEventBus.get().with(tag, EVENT::class.java).post(event)
}

inline fun <reified EVENT> AppCompatActivity.observeEvent(tag: String, crossinline observer: (EVENT) -> Unit) {
    eventObservable<EVENT>(tag).observe(this, Observer {
        observer(it)
    })
}

/**
 * 只能观察相同类型的事件，可用EventMessage
 */
inline fun <reified EVENT> AppCompatActivity.observeEvents(vararg tags: String, crossinline observer: (EVENT) -> Unit) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observe(this, o)
    }
}

inline fun <reified EVENT> AppCompatActivity.observeEventSticky(tag: String, crossinline observer: (EVENT) -> Unit) {
    eventObservable<EVENT>(tag).observeSticky(this, Observer {
        observer(it)
    })
}

inline fun <reified EVENT> AppCompatActivity.observeEventsSticky(
    vararg tags: String,
    crossinline observer: (EVENT) -> Unit
) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observeSticky(this, o)
    }
}

inline fun <reified EVENT> Fragment.observeEvent(tag: String, crossinline observer: (EVENT) -> Unit) {
    eventObservable<EVENT>(tag).observe(this, Observer {
        observer(it)
    })
}

inline fun <reified EVENT> Fragment.observeEvents(vararg tags: String, crossinline observer: (EVENT) -> Unit) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observe(this, o)
    }
}

inline fun <reified EVENT> Fragment.observeEventSticky(tag: String, crossinline observer: (EVENT) -> Unit) {
    eventObservable<EVENT>(tag).observeSticky(this, Observer {
        observer(it)
    })
}

inline fun <reified EVENT> Fragment.observeEventsSticky(vararg tags: String, crossinline observer: (EVENT) -> Unit) {
    val o = Observer<EVENT> {
        observer(it)
    }
    tags.forEach {
        eventObservable<EVENT>(it).observeSticky(this, o)
    }
}
