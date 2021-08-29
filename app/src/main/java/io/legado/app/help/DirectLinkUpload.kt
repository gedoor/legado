package io.legado.app.help

import io.legado.app.help.http.newCallStrResponse
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.postMultipart
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.RuleData

object DirectLinkUpload {

    private const val uploadUrlKey = "directLinkUploadUrl"
    private const val downloadUrlRuleKey = "directLinkDownloadUrlRule"

    suspend fun upLoad(fileName: String, byteArray: ByteArray): String {
        val res = okHttpClient.newCallStrResponse {
            url("https://shuyuan.miaogongzi.site/index.html")
            postMultipart(mapOf("file" to Triple(fileName, byteArray, null)))
        }
        val analyzeRule = AnalyzeRule(RuleData()).setContent(res.body, res.url)
        return analyzeRule.getString("tag.b@text")
    }

    fun getUploadUrl(): String? {
        return CacheManager.get(uploadUrlKey)
    }

    fun putUploadUrl(url: String) {
        CacheManager.put(uploadUrlKey, url)
    }

    fun getDownloadUrlRule(): String? {
        return CacheManager.get(uploadUrlKey)
    }

    fun putDownloadUrlRule(rule: String) {
        CacheManager.put(downloadUrlRuleKey, rule)
    }

    fun delete() {
        CacheManager.delete(uploadUrlKey)
        CacheManager.delete(downloadUrlRuleKey)
    }

}