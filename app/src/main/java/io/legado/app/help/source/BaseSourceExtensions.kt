package io.legado.app.help.source

import io.legado.app.constant.SourceType
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.HttpTTS
import io.legado.app.data.entities.RssSource
import io.legado.app.model.SharedJsScope
import org.mozilla.javascript.Scriptable
import kotlin.coroutines.CoroutineContext

fun BaseSource.getShareScope(coroutineContext: CoroutineContext? = null): Scriptable? {
    return SharedJsScope.getScope(jsLib, coroutineContext)
}

fun BaseSource.getSourceType(): Int {
    return when (this) {
        is BookSource -> SourceType.book
        is RssSource -> SourceType.rss
        else -> error("unknown source type: ${this::class.simpleName}.")
    }
}

fun BaseSource.copy(): BaseSource {
    return when (this) {
        is BookSource -> copy(
            ruleExplore = ruleExplore?.copy(),
            ruleSearch = ruleSearch?.copy(),
            ruleBookInfo = ruleBookInfo?.copy(),
            ruleToc = ruleToc?.copy()
        )

        is RssSource -> copy()
        is HttpTTS -> copy()
        else -> error("unknown copy source type: ${this::class.simpleName}.")
    }
}
