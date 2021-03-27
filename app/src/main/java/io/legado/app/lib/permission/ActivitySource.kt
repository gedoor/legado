package io.legado.app.lib.permission

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

import java.lang.ref.WeakReference

internal class ActivitySource(activity: AppCompatActivity) : RequestSource {

    private val actRef: WeakReference<AppCompatActivity> = WeakReference(activity)

    override val context: Context?
        get() = actRef.get()

    override fun startActivity(intent: Intent) {
        actRef.get()?.startActivity(intent)
    }

}
