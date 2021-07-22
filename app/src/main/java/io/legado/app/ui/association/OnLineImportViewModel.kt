package io.legado.app.ui.association

import io.legado.app.App
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.http.newCall
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray

class OnLineImportViewModel(app: App) : BaseViewModel(app) {

    fun importTextTocRule(url: String, finally: (title: String, msg: String) -> Unit) {
        execute {
            okHttpClient.newCall {
                url(url)
            }.text("utf-8").let { json ->
                GSON.fromJsonArray<TxtTocRule>(json)?.let {
                    appDb.txtTocRuleDao.insert(*it.toTypedArray())
                }
            }
        }.onSuccess {
            finally.invoke(context.getString(R.string.success), "导入Txt规则成功")
        }.onError {
            finally.invoke(context.getString(R.string.error), it.localizedMessage ?: "未知错误")
        }
    }


}