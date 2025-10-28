package io.legado.app.model

import java.util.concurrent.ConcurrentHashMap

object AiSummaryState {
    val inProgress = ConcurrentHashMap.newKeySet<Int>()
}
