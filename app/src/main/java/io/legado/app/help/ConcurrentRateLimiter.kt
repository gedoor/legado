package io.legado.app.help

import io.legado.app.data.entities.BaseSource
import io.legado.app.exception.ConcurrentException
import io.legado.app.model.analyzeRule.AnalyzeUrl.ConcurrentRecord
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

class ConcurrentRateLimiter(val source: BaseSource?) {

    companion object {
        private val concurrentRecordMap = ConcurrentHashMap<String, ConcurrentRecord>()
        /**
         * 更新并发率
         */
        fun updateConcurrentRate(key: String, concurrentRate: String) {
            concurrentRecordMap.compute(key) { _, record ->
                val rateIndex = concurrentRate.indexOf("/")
                if (rateIndex > 0) {
                    val accessLimit = concurrentRate.substring(0, rateIndex).toInt()
                    val interval = concurrentRate.substring(rateIndex + 1).toInt()
                    ConcurrentRecord(true, record?.time?: System.currentTimeMillis(), accessLimit, interval, record?.frequency?: 0)
                }
                else {
                    ConcurrentRecord(false, record?.time?: System.currentTimeMillis(),0,concurrentRate.toInt(), record?.frequency?: 0)
                }
            }
        }
    }

    /**
     * 开始访问,并发判断
     */
    @Throws(ConcurrentException::class)
    private fun fetchStart(): ConcurrentRecord? {
        source ?: return null
        val concurrentRate = source.concurrentRate
        if (concurrentRate.isNullOrEmpty() || concurrentRate == "0") {
            return null
        }
        var isNewRecord = false
        val fetchRecord = concurrentRecordMap.computeIfAbsent(source.getKey()) {
            isNewRecord = true
            val rateIndex = concurrentRate.indexOf("/")
            if (rateIndex > 0) {
                val accessLimit = concurrentRate.substring(0, rateIndex).toInt()
                val interval = concurrentRate.substring(rateIndex + 1).toInt()
                ConcurrentRecord(true, System.currentTimeMillis(), accessLimit, interval, 1)
            }
            else {
                ConcurrentRecord(false, System.currentTimeMillis(),0,concurrentRate.toInt(), 1)
            }
        }
        if (isNewRecord) return fetchRecord
        val waitTime: Int = synchronized(fetchRecord) {
            try {
                if (!fetchRecord.isConcurrent) {
                    //并发控制非 次数/毫秒
                    if (fetchRecord.frequency > 0) {
                        //已经有访问线程,直接等待
                        return@synchronized fetchRecord.interval
                    }
                    //没有线程访问,判断还剩多少时间可以访问
                    val nextTime = fetchRecord.time + fetchRecord.interval
                    if (System.currentTimeMillis() >= nextTime) {
                        fetchRecord.time = System.currentTimeMillis()
                        fetchRecord.frequency = 1
                        return@synchronized 0
                    }
                    return@synchronized (nextTime - System.currentTimeMillis()).toInt()
                } else {
                    //并发控制为 次数/毫秒
                    val nextTime = fetchRecord.time + fetchRecord.interval
                    if (System.currentTimeMillis() >= nextTime) {
                        //已经过了限制时间,重置开始时间
                        fetchRecord.time = System.currentTimeMillis()
                        fetchRecord.frequency = 1
                        return@synchronized 0
                    }
                    if (fetchRecord.frequency > fetchRecord.accessLimit) {
                        return@synchronized (nextTime - System.currentTimeMillis()).toInt()
                    } else {
                        fetchRecord.frequency += 1
                        return@synchronized 0
                    }
                }
            } catch (_: Exception) {
                return@synchronized 0
            }
        }
        if (waitTime > 0) {
            throw ConcurrentException(
                "根据并发率还需等待${waitTime}毫秒才可以访问",
                waitTime = waitTime
            )
        }
        return fetchRecord
    }

    /**
     * 访问结束
     */
    fun fetchEnd(concurrentRecord: ConcurrentRecord?) {
        if (concurrentRecord != null && !concurrentRecord.isConcurrent) {
            synchronized(concurrentRecord) {
                concurrentRecord.frequency -= 1
            }
        }
    }

    /**
     * 获取并发记录，若处于并发限制状态下则会等待
     */
    suspend fun getConcurrentRecord(): ConcurrentRecord? {
        while (true) {
            try {
                return fetchStart()
            } catch (e: ConcurrentException) {
                delay(e.waitTime.toLong())
            }
        }
    }

    fun getConcurrentRecordBlocking(): ConcurrentRecord? {
        while (true) {
            try {
                return fetchStart()
            } catch (e: ConcurrentException) {
                Thread.sleep(e.waitTime.toLong())
            }
        }
    }

    suspend inline fun <T> withLimit(block: () -> T): T {
        val concurrentRecord = getConcurrentRecord()
        try {
            return block()
        } finally {
            fetchEnd(concurrentRecord)
        }
    }

    inline fun <T> withLimitBlocking(block: () -> T): T {
        val concurrentRecord = getConcurrentRecordBlocking()
        try {
            return block()
        } finally {
            fetchEnd(concurrentRecord)
        }
    }

}
