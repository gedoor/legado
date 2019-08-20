package io.legado.app.help

import android.graphics.drawable.Drawable
import com.jayway.jsonpath.JsonPath
import io.legado.app.App
import io.legado.app.utils.readInt
import java.io.File

object ReadBookConfig {
    private val configList = arrayListOf<Config>()
    var styleSelect = 0
    var bg: Drawable? = null

    init {
        val configFile = File(App.INSTANCE.filesDir.absolutePath + File.separator + "config")
        val json = if (configFile.exists()) {
            String(configFile.readBytes())
        } else {
            String(App.INSTANCE.assets.open("defaultConfig.json").readBytes())
        }
        JsonPath.parse(json).let {
            styleSelect = it.readInt("$.readBookSelect") ?: 0
            configList.clear()
            configList.addAll(it.read<Array<Config>>("$.readBook"))
        }
    }

    fun getConfig(): Config {
        return configList[styleSelect]
    }

    data class Config(
        var bg: String = "#F3F3F3",
        var bgInt: Int = 0,
        var bgType: Int = 0,
        var darkStatusIcon: Boolean = true,
        var textColor: String = "#3E3D3B",
        var textSize: Int = 16,
        var letterSpacing: Int = 1,
        var lineSpacingExtra: Int = 15,
        var lineSpacingMultiplier: Int = 3
    )

}