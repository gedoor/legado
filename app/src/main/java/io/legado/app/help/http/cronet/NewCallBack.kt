package io.legado.app.help.http.cronet


import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RequiresApi(api = Build.VERSION_CODES.N)
class NewCallBack(originalRequest: Request, mCall: Call) : AbsCallBack(originalRequest, mCall) {
    private val responseFuture = CompletableFuture<Response>()


    @Throws(IOException::class)
    override fun waitForDone(urlRequest: UrlRequest): Response {
        return responseFuture.get(mCall.timeout().timeoutNanos(), TimeUnit.NANOSECONDS)
    }

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String
    ) {
        super.onRedirectReceived(request, info, newLocationUrl)
        if (mException != null) {
            responseFuture.completeExceptionally(mException)
        }
    }


    override fun onReadCompleted(
        request: UrlRequest,
        info: UrlResponseInfo,
        byteBuffer: ByteBuffer
    ) {
        super.onReadCompleted(request, info, byteBuffer)
        if (mException != null) {
            responseFuture.completeExceptionally(mException)
        }
    }

    override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
        super.onSucceeded(request, info)
        responseFuture.complete(mResponse)
    }

    override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
        super.onFailed(request, info, error)
        responseFuture.completeExceptionally(mException)
    }

    override fun onCanceled(request: UrlRequest?, info: UrlResponseInfo?) {
        super.onCanceled(request, info)
        responseFuture.completeExceptionally(mException)
    }


}