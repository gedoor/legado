package io.legado.app.data.entities.rule

import android.os.Parcel
import android.os.Parcelable

data class BookInfoRule(
    var init: String? = null,
    var name: String? = null,
    var author: String? = null,
    var intro: String? = null,
    var kind: String? = null,
    var lastChapter: String? = null,
    var updateTime: String? = null,
    var coverUrl: String? = null,
    var tocUrl: String? = null,
    var wordCount: String? = null
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(init)
        dest.writeString(name)
        dest.writeString(author)
        dest.writeString(intro)
        dest.writeString(kind)
        dest.writeString(lastChapter)
        dest.writeString(updateTime)
        dest.writeString(coverUrl)
        dest.writeString(tocUrl)
        dest.writeString(wordCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BookInfoRule> {
        override fun createFromParcel(parcel: Parcel): BookInfoRule {
            return BookInfoRule(parcel)
        }

        override fun newArray(size: Int): Array<BookInfoRule?> {
            return arrayOfNulls(size)
        }
    }
}