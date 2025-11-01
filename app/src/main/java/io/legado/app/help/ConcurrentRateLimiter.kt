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
                try {
                    val rateIndex = concurrentRate.indexOf("/")
                    when {
                        rateIndex > 0 -> {
                            val accessLimit = concurrentRate.substring(0, rateIndex).toInt()
                            val interval = concurrentRate.substring(rateIndex + 1).toInt()
                            if (accessLimit <= 0 || interval <= 0) throw NumberFormatException()
                            ConcurrentRecord(
                                record?.time ?: System.currentTimeMillis(),
                                accessLimit,
                                interval,
                                record?.frequency ?: 0
                            )
                        }
                        concurrentRate.toInt() > 0 -> {
                            ConcurrentRecord(
                                record?.time ?: System.currentTimeMillis(),
                                1,
                                concurrentRate.toInt(),
                                record?.frequency ?: 0
                            )
                        }
                        else -> record
                    }
                } catch (e: NumberFormatException) {
                    record
                }
            }
        }
    }

    val concurrentRate = source?.concurrentRate
    val key = source?.getKey()
    /**
     * 开始访问,并发判断
     */
    @Throws(ConcurrentException::class)
    private fun fetchStart(): ConcurrentRecord? {
        source ?: return null
        if (concurrentRate.isNullOrEmpty() || concurrentRate == "0") {
            return null
        }
        var isNewRecord = false
        val fetchRecord = concurrentRecordMap.computeIfAbsent(key!!) {
            isNewRecord = true
            val rateIndex = concurrentRate.indexOf("/")
            if (rateIndex > 0) {
                val accessLimit = concurrentRate.substring(0, rateIndex).toInt()
                val interval = concurrentRate.substring(rateIndex + 1).toInt()
                ConcurrentRecord(System.currentTimeMillis(), accessLimit, interval, 1)
            }
            else {
                ConcurrentRecord(System.currentTimeMillis(),1,concurrentRate.toInt(), 1)
            }
        }
        if (isNewRecord) return fetchRecord
        val waitTime: Long = synchronized(fetchRecord) {
            try {
                //并发控制为 次数/毫秒 , 非并发实际为1/毫秒
                val nextTime = fetchRecord.time + fetchRecord.interval.toLong()
                val nowTime = System.currentTimeMillis()
                if (nowTime >= nextTime) {
                    //已经过了限制时间,重置开始时间
                    fetchRecord.time = nowTime
                    fetchRecord.frequency = 1
                    return@synchronized 0
                }
                if (fetchRecord.frequency < fetchRecord.accessLimit) {
                    fetchRecord.frequency += 1
                    return@synchronized 0
                } else {
                    return@synchronized nextTime - nowTime
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
     * 获取并发记录，若处于并发限制状态下则会等待
     */
    suspend fun getConcurrentRecord(): ConcurrentRecord? {
        while (true) {
            try {
                return fetchStart()
            } catch (e: ConcurrentException) {
                delay(e.waitTime)
            }
        }
    }

    fun getConcurrentRecordBlocking(): ConcurrentRecord? {
        while (true) {
            try {
                return fetchStart()
            } catch (e: ConcurrentException) {
                Thread.sleep(e.waitTime)
            }
        }
    }

    suspend inline fun <T> withLimit(block: () -> T): T {
        getConcurrentRecord()
        return block()
    }

    inline fun <T> withLimitBlocking(block: () -> T): T {
        getConcurrentRecordBlocking()
        return block()
    }

}
