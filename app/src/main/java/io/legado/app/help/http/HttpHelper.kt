package io.legado.app.help.http

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("unused")
object HttpHelper {

    
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
