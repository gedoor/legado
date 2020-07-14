package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ContentRule(
    var content: String? = null,
    var nextContentUrl: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null
) : Parcelable