package io.legado.app.data.entities.rule

import android.os.Parcel
import android.os.Parcelable

data class TocRule(
    var chapterList: String? = null,
    var chapterName: String? = null,
    var chapterUrl: String? = null,
    var isVip: String? = null,
    var updateTime: String? = null,
    var nextTocUrl: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(chapterList)
        dest.writeString(chapterName)
        dest.writeString(chapterUrl)
        dest.writeString(isVip)
        dest.writeString(updateTime)
        dest.writeString(nextTocUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TocRule> {
        override fun createFromParcel(parcel: Parcel): TocRule {
            return TocRule(parcel)
        }

        override fun newArray(size: Int): Array<TocRule?> {
            return arrayOfNulls(size)
        }
    }
}