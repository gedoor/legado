package io.legado.app.help.permission

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager

interface RequestSource {

    val context: Context?

    fun startActivity(intent: Intent)

}
