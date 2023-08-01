package io.legado.app.help

import androidx.annotation.Keep
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.utils.ACache
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.compress.ZipUtils
import io.legado.app.utils.createFileReplace
import io.legado.app.utils.externalCache
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import splitties.init.appCtx
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
object DirectLinkUpload {

    const val ruleFileName = "directLinkUploadRule.json"

    @Throws(NoStackTraceException::class)
    suspend fun upLoad(
        fileName: String,
        file: Any,
        contentType: String,
        rule: Rule = getRule()
    ): String {
        val url = rule.uploadUrl
        if (url.isBlank()) {
            throw NoStackTraceException("上传url未配置")
        }
        val downloadUrlRule = rule.downloadUrlRule
        if (downloadUrlRule.isBlank()) {
            throw NoStackTraceException("下载地址规则未配置")
        }
        var mFileName = fileName
        var mFile = file
        var mContentType = contentType
        if (rule.compress && contentType != "application/zip") {
            mFileName = "$fileName.zip"
            mContentType = "application/zip"
            mFile = when (file) {
                is File -> {
                    val zipFile = File(FileUtils.getPath(appCtx.externalCache, "upload", mFileName))
                    zipFile.createFileReplace()
                    ZipUtils.zipFile(file, zipFile)
                    zipFile
                }

                is ByteArray -> ZipUtils.zipByteArray(file, fileName)
                is String -> ZipUtils.zipByteArray(file.toByteArray(), fileName)
                else -> ZipUtils.zipByteArray(GSON.toJson(file).toByteArray(), fileName)
            }
        }
        val analyzeUrl = AnalyzeUrl(url)
        val res = analyzeUrl.upload(mFileName, mFile, mContentType)
        if (mFile is File) {
            mFile.delete()
        }
        val analyzeRule = AnalyzeRule().setContent(res.body, res.url)
        val downloadUrl = analyzeRule.getString(downloadUrlRule)
        if (downloadUrl.isBlank()) {
            throw NoStackTraceException("上传失败,${res.body}")
        }
        return downloadUrl
    }

    val defaultRules: List<Rule> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}directLinkUpload.json")
                .readBytes()
        )
        GSON.fromJsonArray<Rule>(json).getOrThrow()
    }

    fun getRule(): Rule {
        return getConfig() ?: defaultRules[0]
    }

    fun getConfig(): Rule? {
        val json = ACache.get(cacheDir = false).getAsString(ruleFileName)
        return GSON.fromJsonObject<Rule>(json).getOrNull()
    }

    fun putConfig(rule: Rule) {
        ACache.get(cacheDir = false).put(ruleFileName, GSON.toJson(rule))
    }

    fun delConfig() {
        ACache.get(cacheDir = false).remove(ruleFileName)
    }

    fun getSummary(): String {
        return getRule().summary
    }

    @Keep
    data class Rule(
        var uploadUrl: String, //上传url
        var downloadUrlRule: String, //下载链接规则
        var summary: String, //注释
        var compress: Boolean = false, //是否压缩
    ) {

        override fun toString(): String {
            return summary
        }

    }

}
