package io.legado.app.lib.cronet

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.UrlRequest
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SuppressLint("ObsoleteSdkInt")
@Keep
@RequiresApi(api = Build.VERSION_CODES.N)
class NewCallBack(originalRequest: Request, mCall: Call, readTimeoutMillis: Int) :
    AbsCallBack(originalRequest, mCall, readTimeoutMillis) {

    private val responseFuture = CompletableFuture<Response>()

    @Throws(IOException::class)
    override fun waitForDone(urlRequest: UrlRequest): Response {
        urlRequest.start()
        startCheckCancelJob(urlRequest)
        //DebugLog.i(javaClass.simpleName, "start ${originalRequest.method} ${originalRequest.url}")
        return if (mCall.timeout().timeoutNanos() > 0) {
            responseFuture.get(mCall.timeout().timeoutNanos(), TimeUnit.NANOSECONDS)
        } else {
            return responseFuture.get()
        }

    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     * @param error
     */
    override fun onError(error: IOException) {
        responseFuture.completeExceptionally(error)
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     * @param response
     */
    override fun onSuccess(response: Response) {
        responseFuture.complete(response)
    }


}