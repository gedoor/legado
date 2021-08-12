package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LogInRule(
    val ui: HashMap<String, String>? = null,
    val url: String? = null,
    val checkJs: String? = null
) : Parcelable