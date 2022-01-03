package io.legado.app.help

import io.legado.app.data.appDb
import io.legado.app.data.entities.HttpTTS
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonArray
import splitties.init.appCtx
import java.io.File

object DefaultData {

    const val httpTtsFileName = "httpTTS.json"
    const val txtTocRuleFileName = "txtTocRule.json"

    val httpTTS: List<HttpTTS> by lazy {
        val json =
            String(
                appCtx.assets.open("defaultData${File.separator}$httpTtsFileName")
                    .readBytes()
            )
        HttpTTS.fromJsonArray(json)
    }

    val readConfigs: List<ReadBookConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray(json)!!
    }

    val txtTocRules: List<TxtTocRule> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}$txtTocRuleFileName")
                .readBytes()
        )
        GSON.fromJsonArray(json)!!
    }

    val themeConfigs: List<ThemeConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ThemeConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray(json)!!
    }

    val rssSources: List<RssSource> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}rssSources.json")
                .readBytes()
        )
        RssSource.fromJsonArray(json)
    }

    fun importDefaultHttpTTS() {
        appDb.httpTTSDao.deleteDefault()
        appDb.httpTTSDao.insert(*httpTTS.toTypedArray())
    }

    fun importDefaultTocRules() {
        appDb.txtTocRuleDao.deleteDefault()
        appDb.txtTocRuleDao.insert(*txtTocRules.toTypedArray())
    }

    fun importDefaultRssSources() {
        appDb.rssSourceDao.insert(*rssSources.toTypedArray())
    }

}