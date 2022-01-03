package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RowUi(
    var name: String,
    var type: String?,
    var action: String?
) : Parcelable