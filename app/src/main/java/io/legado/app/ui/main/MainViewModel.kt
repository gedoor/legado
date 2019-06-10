package io.legado.app.ui.main

import android.app.Application
import io.legado.app.App
import io.legado.app.base.BaseViewModel
import kotlinx.coroutines.async
import org.jetbrains.anko.toast

class MainViewModel(application: Application) : BaseViewModel(application) {

    fun test() {
        launchOnUI({

            val result = async {
                "结果"
            }

//            App.INSTANCE.toast("result: $result")
        })
    }

}