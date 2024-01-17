package io.legado.app.lib.cronet

import android.os.ConditionVariable
import androidx.annotation.Keep
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.UrlRequest
import java.io.IOException

@Keep
class OldCallback(originalRequest: Request, mCall: Call, readTimeoutMillis: Int) :
    AbsCallBack(originalRequest, mCall, readTimeoutMillis) {

    private val mResponseCondition = ConditionVariable()
    private var mException: IOException? = null

    @Throws(IOException::class)
    override fun waitForDone(urlRequest: UrlRequest): Response {
        //获取okhttp call的完整请求的超时时间
        val timeOutMs: Long = mCall.timeout().timeoutNanos() / 1000000
        urlRequest.start()
        startCheckCancelJob(urlRequest)
        if (timeOutMs > 0) {
            mResponseCondition.block(timeOutMs)
        } else {
            mResponseCondition.block()
        }
        //ConditionVariable 正常open或者超时open后，检查urlRequest是否完成
        if (!urlRequest.isDone) {
            urlRequest.cancel()
            mException = IOException("Cronet timeout after wait " + timeOutMs + "ms")
        }

        if (mException != null) {
            throw mException as IOException
        }
        return mResponse
    }

    /**
     * 当发生错误时，通知子类终止阻塞抛出错误
     * @param error
     */
    override fun onError(error: IOException) {
        mException = error
        mResponseCondition.open()
    }

    /**
     * 请求成功后，通知子类结束阻塞，返回response
     * @param response
     */
    override fun onSuccess(response: Response) {
        mResponseCondition.open()
    }


}