package io.legado.app.lib.permission

import android.content.Context
import android.content.Intent

interface RequestSource {

    val context: Context?

    fun startActivity(intent: Intent)

}
