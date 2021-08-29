package io.legado.app.lib.dialogs

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Suppress("unused")
@Parcelize
data class SelectItem(
    val title: String,
    val id: Int
) : Parcelable {

    override fun toString(): String {
        return title
    }

}
