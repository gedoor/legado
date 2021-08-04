package io.legado.app.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentRule(
    var content: String? = null,
    var nextContentUrl: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null,
    var replaceRegex: String? = null, //替换规则
    var imageStyle: String? = null,  //默认大小居中,FULL最大宽度
) : Parcelable