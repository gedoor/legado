package io.legado.app.help.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue

class OrderCoroutine<T>(val threadCount: Int) {
    private val taskList = ConcurrentLinkedQueue<suspend CoroutineScope.() -> T>()
    private val taskResultMap = ConcurrentHashMap<Int, T>()
    private val finishTaskIndex = PriorityBlockingQueue<Int>()
    private val mutex = Mutex()

    private suspend fun start() = coroutineScope {
        var taskIndex = 0
        for (i in 1..threadCount) {
            launch {
                while (true) {
                    ensureActive()
                    val task: suspend CoroutineScope.() -> T
                    val curIndex: Int
                    mutex.withLock {
                        task = taskList.poll() ?: return@launch
                        curIndex = taskIndex++
                    }
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