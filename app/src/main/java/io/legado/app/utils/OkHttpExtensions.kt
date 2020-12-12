package io.legado.app.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.await(): Response = suspendCancellableCoroutine { block ->

    block.invokeOnCancellation {
        cancel()
    }

    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            block.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            block.resume(response)
        }
    })

}