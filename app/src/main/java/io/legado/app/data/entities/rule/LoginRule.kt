package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LoginRule(
    var ui: List<RowUi>? = null,
    var url: String? = null, //url或js,js必须使用js标记
    var checkJs: String? = null, //只能写js,不用js标记
) : Parcelable {

    @Parcelize
    data class RowUi(
        var name: String,
        var type: String,
    ) : Parcelable


}