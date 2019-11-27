package io.legado.app.service.help

import androidx.lifecycle.MutableLiveData
import io.legado.app.data.entities.Book
import io.legado.app.model.WebBook
import io.legado.app.ui.book.read.ReadBookViewModel
import io.legado.app.ui.widget.page.TextChapter


object ReadBook {

    var titleDate = MutableLiveData<String>()
    var book: Book? = null
    var inBookshelf = false
    var chapterSize = 0
    var callBack: ReadBookViewModel.CallBack? = null
    var durChapterIndex = 0
    var durPageIndex = 0
    var isLocalBook = true
    var prevTextChapter: TextChapter? = null
    var curTextChapter: TextChapter? = null
    var nextTextChapter: TextChapter? = null
    var webBook: WebBook? = null
    val loadingChapters = arrayListOf<Int>()


}