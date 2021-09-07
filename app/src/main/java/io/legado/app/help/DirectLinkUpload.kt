package io.legado.app.help

import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData

object DirectLinkUpload {

    private const val uploadUrlKey = "directLinkUploadUrl"
    private const val downloadUrlRuleKey = "directLinkDownloadUrlRule"

    suspend fun upLoad(fileName: String, file: ByteArray, contentType: String): String {
        val url = getUploadUrl()
        if (url.isNullOrBlank()) {
            error("上传url未配置")
        }
        val downloadUrlRule = getDownloadUrlRule()
        if (downloadUrlRule.isNullOrBlank()) {
            error("下载地址规则未配置")
        }
        val analyzeUrl = AnalyzeUrl(url)
        val res = analyzeUrl.upload(fileName, file, contentType)
        val analyzeRule = AnalyzeRule(RuleData()).setContent(res.body, res.url)
        val downloadUrl = analyzeRule.getString(downloadUrlRule)
        if (downloadUrl.isBlank()) {
            error("上传失败,${res.body}")
        }
        return downloadUrl
    }

    fun getUploadUrl(): String {
        return CacheManager.get(uploadUrlKey)
            ?: """http://shuyuan.miaogongzi.site:6564/shuyuan,{
            "method":"POST",
            "body": {
                "file": "fileRequest"
            },
            "type": "multipart/form-data"
          }""".trimMargin()
    }

    fun putUploadUrl(url: String) {
        CacheManager.put(uploadUrlKey, url)
    }

    fun getDownloadUrlRule(): String {
        return CacheManager.get(downloadUrlRuleKey)
            ?: """
                ${'$'}.data@js:if (result == '') '' 
                else 'https://shuyuan.miaogongzi.site/shuyuan/'+result
            """.trimIndent()
    }

    fun putDownloadUrlRule(rule: String) {
        CacheManager.put(downloadUrlRuleKey, rule)
    }

    fun delete() {
        CacheManager.delete(uploadUrlKey)
        CacheManager.delete(downloadUrlRuleKey)
    }

}