package io.legado.app.ui.login

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var bookSource: BookSource? = null

    fun initData(sourceUrl: String, success: (bookSource: BookSource) -> Unit) {
        execute {
            bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            bookSource
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("未找到书源")
            }
        }
    }

}