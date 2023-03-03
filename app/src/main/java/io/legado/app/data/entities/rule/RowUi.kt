package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RowUi(
    var name: String,
    var type: String = "text",
    var action: String? = null
) : Parcelable {

    object Type {

        const val text = "text"
        const val password = "password"
        const val button = "button"

    }

}