package io.legado.app.ui.changecover

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel

class ChangeCoverViewModel(application: Application) : BaseViewModel(application) {

    var name: String = ""
    var author: String = ""

    fun initData() {
        execute {
            App.db.searchBookDao().getByNameAuthorEnable(name, author).let {

            }
        }
    }

    interface CallBack {
        var adapter: CoverAdapter
    }
}