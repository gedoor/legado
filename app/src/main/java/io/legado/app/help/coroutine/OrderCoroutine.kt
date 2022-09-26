package io.legado.app.help.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

class OrderCoroutine<T>(val threadCount: Int) {
    private val taskList = ArrayList<suspend CoroutineScope.() -> T>()
    private val taskResultMap = ConcurrentHashMap<Int, T>()
    private val finishTaskIndex = PriorityBlockingQueue<Int>()

    private suspend fun start() = coroutineScope {
        val taskIndex = AtomicInteger(0)
        val tasks = taskList.toList()
        for (i in 1..threadCount) {
            launch {
                while (true) {
                    ensureActive()
                    val curIndex = taskIndex.getAndIncrement()
                    val task = tasks.getOrNull(curIndex) ?: return@launch
                    taskResultMap[curIndex] = task.invoke(this)
                    finishTaskIndex.add(curIndex)
                }
            }
        }
    }

    fun submit(block: suspend CoroutineScope.() -> T) {
        taskList.add(block)
    }

    suspend fun collect(block: (index: Int, result: T) -> Unit) = withContext(IO) {
        var index = 0
        val taskSize = taskList.size
        launch { start() }
        while (index < taskSize) {
            ensureActive()
            if (finishTaskIndex.peek() == index) {
                finishTaskIndex.poll()
                block.invoke(index, taskResultMap.remove(index)!!)
                index++
            }
        }
    }

}