package io.legado.app.data.entities.rule

import android.os.Parcel
import android.os.Parcelable

data class ContentRule(
    var content: String? = null,
    var nextContentUrl: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(content)
        dest.writeString(nextContentUrl)
        dest.writeString(webJs)
        dest.writeString(sourceRegex)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ContentRule> {
        override fun createFromParcel(parcel: Parcel): ContentRule {
            return ContentRule(parcel)
        }

        override fun newArray(size: Int): Array<ContentRule?> {
            return arrayOfNulls(size)
        }
    }
}