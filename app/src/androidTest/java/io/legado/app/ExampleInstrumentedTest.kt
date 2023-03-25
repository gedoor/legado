package io.legado.app

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testContentProvider() {
        // Context of the app under test.
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        Log.d(
            "test",
            appContext.contentResolver.query(
                Uri.parse("content://io.legado.app.api.ReaderProvider/sources/query"),
                null,
                null,
                null,
                null
            )
            !!.getString(0)
        )
    }
}
