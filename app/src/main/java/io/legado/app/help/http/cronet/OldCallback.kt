package io.legado.app.help.http.cronet

import android.os.ConditionVariable
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.nio.ByteBuffer

class OldCallback(originalRequest: Request, mCall: Call) : AbsCallBack(originalRequest, mCall) {

    private val mResponseCondition = ConditionVariable()

    @Throws(IOException::class)
    override fun waitForDone(urlRequest: UrlRequest): Response {
        //获取okhttp call的完整请求的超时时间
        val timeOutMs: Long = mCall.timeout().timeoutNanos() / 1000000
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
        return this.mResponse
    }


    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer
    ) {
        super.onReadCompleted(request, info, byteBuffer)
        if (mException != null) {
            mResponseCondition.open()
        }
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        super.onSucceeded(request, info)
        mResponseCondition.open()
    }

    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
        super.onFailed(request, info, error)
        mResponseCondition.open()
    }


    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        super.onCanceled(request, info)
        mResponseCondition.open()
    }

}