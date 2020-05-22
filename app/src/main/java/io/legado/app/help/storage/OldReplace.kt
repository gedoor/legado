package io.legado.app.help.storage

import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.*

object OldReplace {

    fun jsonToReplaceRule(json: String): ReplaceRule? {
        var replaceRule: ReplaceRule? = null
        runCatching {
            replaceRule = GSON.fromJsonObject<ReplaceRule>(json.trim())
        }
        runCatching {
            if (replaceRule == null || replaceRule?.pattern.isNullOrBlank()) {
                val jsonItem = Restore.jsonPath.parse(json.trim())
                val rule = ReplaceRule()
                rule.id = jsonItem.readLong("$.id") ?: System.currentTimeMillis()
                rule.pattern = jsonItem.readString("$.regex") ?: ""
                if (rule.pattern.isEmpty()) return null
                rule.name = jsonItem.readString("$.replaceSummary") ?: ""
                rule.replacement = jsonItem.readString("$.replacement") ?: ""
                rule.isRegex = jsonItem.readBool("$.isRegex") == true
                rule.scope = jsonItem.readString("$.useTo")
                rule.isEnabled = jsonItem.readBool("$.enable") == true
                rule.order = jsonItem.readInt("$.serialNumber") ?: 0
                return rule
            }
        }
        return replaceRule
    }

}