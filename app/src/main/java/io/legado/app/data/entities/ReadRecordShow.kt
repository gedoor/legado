package io.legado.app.data.entities

import androidx.room.Ignore

data class ReadRecordShow(
    var bookName: String = "",
    var readTime: Long = 0L
) {

    @Ignore
    constructor() : this("")

}