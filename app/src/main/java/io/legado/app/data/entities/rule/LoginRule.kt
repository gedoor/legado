package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginRule(
    var ui: List<RowUi>? = null,
    var url: String? = null,
    var checkJs: String? = null
) : Parcelable {

    @Parcelize
    data class RowUi(
        var name: String,
        var type: String,
    ) : Parcelable


}