package io.legado.app.data.entities.rule

import android.os.Parcel
import android.os.Parcelable

data class ExploreRule(
    override var bookList: String? = null,
    override var name: String? = null,
    override var author: String? = null,
    override var intro: String? = null,
    override var kind: String? = null,
    override var lastChapter: String? = null,
    override var updateTime: String? = null,
    override var bookUrl: String? = null,
    override var coverUrl: String? = null,
    override var wordCount: String? = null
) : BookListRule, Parcelable {

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
        dest.writeString(bookList)
        dest.writeString(name)
        dest.writeString(author)
        dest.writeString(intro)
        dest.writeString(kind)
        dest.writeString(lastChapter)
        dest.writeString(updateTime)
        dest.writeString(bookUrl)
        dest.writeString(coverUrl)
        dest.writeString(wordCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExploreRule> {
        override fun createFromParcel(parcel: Parcel): ExploreRule {
            return ExploreRule(parcel)
        }

        override fun newArray(size: Int): Array<ExploreRule?> {
            return arrayOfNulls(size)
        }
    }
}

