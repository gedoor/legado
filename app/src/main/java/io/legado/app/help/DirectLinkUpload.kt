package io.legado.app.help

import io.legado.app.model.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.jsonPath
import io.legado.app.utils.readString
import splitties.init.appCtx
import java.io.File

object DirectLinkUpload {

    private const val uploadUrlKey = "directLinkUploadUrl"
    private const val downloadUrlRuleKey = "directLinkDownloadUrlRule"
    private const val summaryKey = "directSummary"

    suspend fun upLoad(fileName: String, file: ByteArray, contentType: String): String {
        val url = getUploadUrl()
        if (url.isNullOrBlank()) {
            throw NoStackTraceException("上传url未配置")
        }
        val downloadUrlRule = getDownloadUrlRule()
        if (downloadUrlRule.isNullOrBlank()) {
            throw NoStackTraceException("下载地址规则未配置")
        }
        val analyzeUrl = AnalyzeUrl(url)
        val res = analyzeUrl.upload(fileName, file, contentType)
        val analyzeRule = AnalyzeRule(RuleData()).setContent(res.body, res.url)
        val downloadUrl = analyzeRule.getString(downloadUrlRule)
        if (downloadUrl.isBlank()) {
            throw NoStackTraceException("上传失败,${res.body}")
        }
        return downloadUrl
    }

    private val ruleDoc by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}directLinkUpload.json")
                .readBytes()
        )
        jsonPath.parse(json)
    }

    fun getUploadUrl(): String? {
        return CacheManager.get(uploadUrlKey)
            ?: ruleDoc.readString("$.UploadUrl")
    }

    fun putUploadUrl(url: String) {
        CacheManager.put(uploadUrlKey, url)
    }

    fun getDownloadUrlRule(): String? {
        return CacheManager.get(downloadUrlRuleKey)
            ?: ruleDoc.readString("$.DownloadUrlRule")
    }

    fun putDownloadUrlRule(rule: String) {
        CacheManager.put(downloadUrlRuleKey, rule)
    }

    fun getSummary(): String? {
        return CacheManager.get(summaryKey)
            ?: ruleDoc.readString("summary")
    }

    fun putSummary(summary: String?) {
        if (summary != null) {
            CacheManager.put(summaryKey, summary)
        } else {
            CacheManager.delete(summaryKey)
        }
    }

    fun delete() {
        CacheManager.delete(uploadUrlKey)
        CacheManager.delete(downloadUrlRuleKey)
        CacheManager.delete(summaryKey)
    }

}