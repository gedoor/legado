package io.legado.app.utils

import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

fun <I, O> AppCompatActivity.registerForActivityResult(contract: ActivityResultContract<I, O>): ActivityResultLauncherAwait<I, O> {
    lateinit var cout: CancellableContinuation<O>
    val launcher = registerForActivityResult(contract) {
        if (cout.isActive) {
            cout.resume(it)
        }
    }
    return object : ActivityResultLauncherAwait<I, O>() {
        override suspend fun launch(input: I, options: ActivityOptionsCompat?): O {
            return suspendCancellableCoroutine {
                cout = it
                launcher.launch(input, options)
            }
        }

        override fun unregister() {
            launcher.unregister()
        }

        override fun getContract(): ActivityResultContract<I, *> {
            return launcher.contract
        }
    }
}

abstract class ActivityResultLauncherAwait<I, O> {

    suspend fun launch(input: I): O {
        return launch(input, null)
    }

    abstract suspend fun launch(input: I, options: ActivityOptionsCompat?): O

    abstract fun unregister()

    abstract fun getContract(): ActivityResultContract<I, *>

}
