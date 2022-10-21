package io.legado.app.api.controller

import io.legado.app.api.ReturnData
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject

object ReplaceRuleController {

    val allRules get() = appDb.replaceRuleDao.all


    fun saveRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        val rule = GSON.fromJsonObject<ReplaceRule>(postData).getOrNull()
        if (rule == null) {
            returnData.setErrorMsg("格式不对")
        } else {
            if (rule.order == Int.MIN_VALUE) {
                rule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(rule)
        }
        return returnData
    }

    fun testRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        val map = GSON.fromJsonObject<Map<String, *>>(postData).getOrNull()

        return returnData
    }

}