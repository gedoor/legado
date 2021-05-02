package io.legado.app.help.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume

@Suppress("unused")
object HttpHelper {

    suspend fun newCall(builder: Request.Builder.() -> Unit, proxy: String? = null): Response {
        val client = OkHttpHelper.getProxyClient(proxy)
        val requestBuilder = Request.Builder().apply(builder)
        return client.newCall(requestBuilder.build()).await()
    }

    suspend fun ajax(params: AjaxWebView.AjaxParams): StrResponse =
        suspendCancellableCoroutine { block ->
            val webView = AjaxWebView()
            block.invokeOnCancellation {
                webView.destroyWebView()
            }
            webView.callback = object : AjaxWebView.Callback() {
                override fun onResult(response: StrResponse) {

                    if (!block.isCompleted)
                        block.resume(response)
                }

                override fun onError(error: Throwable) {
                    if (!block.isCompleted)
                        block.cancel(error)
                }
            }
            webView.load(params)
        }

}
