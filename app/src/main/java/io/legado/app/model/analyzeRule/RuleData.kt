package io.legado.app.model.analyzeRule

import io.legado.app.constant.AppLog

class RuleData : RuleDataInterface {

    override val variableMap by lazy {
        hashMapOf<String, String>()
    }

    override fun putVariable(key: String, value: String?) {
        if (value != null) {
            if (value.length > 1000) {
                AppLog.put("设置变量长度超过1000,设置失败")
                return
            }
            variableMap[key] = value
        } else {
            variableMap.remove(key)
        }
    }

}