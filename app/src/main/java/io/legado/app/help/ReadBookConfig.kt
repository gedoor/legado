package io.legado.app.help

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.ui.book.read.page.ChapterProvider
import io.legado.app.utils.*
import java.io.File

/**
 * 阅读界面配置
 */
@Keep
object ReadBookConfig {
    const val readConfigFileName = "readConfig.json"
    private val configFilePath =
        App.INSTANCE.filesDir.absolutePath + File.separator + readConfigFileName
    val configList: ArrayList<Config> = arrayListOf()
    private val defaultConfigs by lazy {
        val json = String(App.INSTANCE.assets.open(readConfigFileName).readBytes())
        GSON.fromJsonArray<Config>(json)!!
    }
    val durConfig get() = getConfig(styleSelect)
    private val shareConfig get() = getConfig(5)
    var bg: Drawable? = null
    var bgMeanColor: Int = 0

    init {
        upConfig()
    }

    @Synchronized
    fun getConfig(index: Int): Config {
        if (configList.size < 5) {
            resetAll()
        }
        if (configList.size < 6) {
            configList.add(Config())
        }
        return configList[index]
    }

    fun upConfig() {
        (getConfigs() ?: defaultConfigs).let {
            configList.clear()
            configList.addAll(it)
        }
    }

    private fun getConfigs(): List<Config>? {
        val configFile = File(configFilePath)
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                return GSON.fromJsonArray(json)
            } catch (e: Exception) {
            }
        }
        return null
    }

    fun upBg() {
        val resources = App.INSTANCE.resources
        val dm = resources.displayMetrics
        val width = dm.widthPixels
        val height = dm.heightPixels
        bg = durConfig.bgDrawable(width, height)
        bg?.let {
            if (it is BitmapDrawable) {
                bgMeanColor = BitmapUtils.getMeanColor(it.bitmap)
            } else if (it is ColorDrawable) {
                bgMeanColor = it.color
            }
        }
    }

    fun save() {
        Coroutine.async {
            val json = GSON.toJson(configList)
            FileUtils.createFileIfNotExist(configFilePath).writeText(json)
        }
    }

    fun resetDur() {
        defaultConfigs[styleSelect].let {
            durConfig.setBg(it.bgType(), it.bgStr())
            durConfig.setTextColor(it.textColor())
            upBg()
            save()
        }
    }

    private fun resetAll() {
        defaultConfigs.let {
            configList.clear()
            configList.addAll(it)
            save()
        }
    }

    //配置写入读取
    var styleSelect = App.INSTANCE.getPrefInt(PreferKey.readStyleSelect)
        set(value) {
            field = value
            if (App.INSTANCE.getPrefInt(PreferKey.readStyleSelect) != value) {
                App.INSTANCE.putPrefInt(PreferKey.readStyleSelect, value)
            }
        }
    var shareLayout = App.INSTANCE.getPrefBoolean(PreferKey.shareLayout)
        set(value) {
            field = value
            if (App.INSTANCE.getPrefBoolean(PreferKey.shareLayout) != value) {
                App.INSTANCE.putPrefBoolean(PreferKey.shareLayout, value)
            }
        }
    var pageAnim = App.INSTANCE.getPrefInt(PreferKey.pageAnim)
        set(value) {
            field = value
            isScroll = value == 3
            if (App.INSTANCE.getPrefInt(PreferKey.pageAnim) != value) {
                App.INSTANCE.putPrefInt(PreferKey.pageAnim, value)
            }
        }
    var isScroll = pageAnim == 3
    val clickTurnPage get() = App.INSTANCE.getPrefBoolean(PreferKey.clickTurnPage, true)
    var bodyIndentCount = App.INSTANCE.getPrefInt(PreferKey.bodyIndent, 2)
        set(value) {
            field = value
            bodyIndent = "　".repeat(value)
            if (App.INSTANCE.getPrefInt(PreferKey.bodyIndent, 2) != value) {
                App.INSTANCE.putPrefInt(PreferKey.bodyIndent, value)
            }
        }
    var bodyIndent = "　".repeat(bodyIndentCount)
    var hideStatusBar = App.INSTANCE.getPrefBoolean(PreferKey.hideStatusBar)
    var hideNavigationBar = App.INSTANCE.getPrefBoolean(PreferKey.hideNavigationBar)
    var textBold: Boolean
        get() = if (shareLayout) shareConfig.textBold else durConfig.textBold
        set(value) = if (shareLayout) shareConfig.textBold = value else durConfig.textBold = value

    var textSize: Int
        get() = if (shareLayout) shareConfig.textSize else durConfig.textSize
        set(value) = if (shareLayout) shareConfig.textSize = value else durConfig.textSize = value

    var letterSpacing: Float
        get() = if (shareLayout) shareConfig.letterSpacing else durConfig.letterSpacing
        set(value) =
            if (shareLayout) shareConfig.letterSpacing = value else durConfig.letterSpacing = value

    var lineSpacingExtra: Int
        get() = if (shareLayout) shareConfig.lineSpacingExtra else durConfig.lineSpacingExtra
        set(value) =
            if (shareLayout) shareConfig.lineSpacingExtra = value
            else durConfig.lineSpacingExtra = value

    var paragraphSpacing: Int
        get() = if (shareLayout) shareConfig.paragraphSpacing else durConfig.paragraphSpacing
        set(value) =
            if (shareLayout) shareConfig.paragraphSpacing = value
            else durConfig.paragraphSpacing = value

    var titleMode: Int
        get() = if (shareLayout) shareConfig.titleMode else durConfig.titleMode
        set(value) =
            if (shareLayout) shareConfig.titleMode = value else durConfig.titleMode = value

    var paddingBottom: Int
        get() = if (shareLayout) shareConfig.paddingBottom else durConfig.paddingBottom
        set(value) =
            if (shareLayout) shareConfig.paddingBottom = value else durConfig.paddingBottom = value

    var paddingLeft: Int
        get() = if (shareLayout) shareConfig.paddingLeft else durConfig.paddingLeft
        set(value) =
            if (shareLayout) shareConfig.paddingLeft = value else durConfig.paddingLeft = value

    var paddingRight: Int
        get() = if (shareLayout) shareConfig.paddingRight else durConfig.paddingRight
        set(value) =
            if (shareLayout) shareConfig.paddingRight = value else durConfig.paddingRight = value

    var paddingTop: Int
        get() = if (shareLayout) shareConfig.paddingTop else durConfig.paddingTop
        set(value) =
            if (shareLayout) shareConfig.paddingTop = value else durConfig.paddingTop = value

    var headerPaddingBottom: Int
        get() = if (shareLayout) shareConfig.headerPaddingBottom else durConfig.headerPaddingBottom
        set(value) =
            if (shareLayout) shareConfig.headerPaddingBottom = value
            else durConfig.headerPaddingBottom = value

    var headerPaddingLeft: Int
        get() = if (shareLayout) shareConfig.headerPaddingLeft else durConfig.headerPaddingLeft
        set(value) =
            if (shareLayout) shareConfig.headerPaddingLeft = value
            else durConfig.headerPaddingLeft = value

    var headerPaddingRight: Int
        get() = if (shareLayout) shareConfig.headerPaddingRight else durConfig.headerPaddingRight
        set(value) =
            if (shareLayout) shareConfig.headerPaddingRight = value
            else durConfig.headerPaddingRight = value

    var headerPaddingTop: Int
        get() = if (shareLayout) shareConfig.headerPaddingTop else durConfig.headerPaddingTop
        set(value) =
            if (shareLayout) shareConfig.headerPaddingTop = value
            else durConfig.headerPaddingTop = value

    var footerPaddingBottom: Int
        get() = if (shareLayout) shareConfig.footerPaddingBottom else durConfig.footerPaddingBottom
        set(value) =
            if (shareLayout) shareConfig.footerPaddingBottom = value
            else durConfig.footerPaddingBottom = value

    var footerPaddingLeft: Int
        get() = if (shareLayout) shareConfig.footerPaddingLeft else durConfig.footerPaddingLeft
        set(value) =
            if (shareLayout) shareConfig.footerPaddingLeft = value
            else durConfig.footerPaddingLeft = value

    var footerPaddingRight: Int
        get() = if (shareLayout) shareConfig.footerPaddingRight else durConfig.footerPaddingRight
        set(value) =
            if (shareLayout) shareConfig.footerPaddingRight = value
            else durConfig.footerPaddingRight = value

    var footerPaddingTop: Int
        get() = if (shareLayout) shareConfig.footerPaddingTop else durConfig.footerPaddingTop
        set(value) =
            if (shareLayout) shareConfig.footerPaddingTop = value
            else durConfig.footerPaddingTop = value

    @Keep
    class Config(
        private var bgStr: String = "#EEEEEE",//白天背景
        private var bgStrNight: String = "#000000",//夜间背景
        private var bgType: Int = 0,//白天背景类型 0:颜色, 1:assets图片, 2其它图片
        private var bgTypeNight: Int = 0,//夜间背景类型
        private var darkStatusIcon: Boolean = true,//白天是否暗色状态栏
        private var darkStatusIconNight: Boolean = false,//晚上是否暗色状态栏
        private var textColor: String = "#3E3D3B",//白天文字颜色
        private var textColorNight: String = "#ADADAD",//夜间文字颜色
        var textBold: Boolean = false,//是否粗体字
        var textSize: Int = 20,//文字大小
        var letterSpacing: Float = 0.5f,//字间距
        var lineSpacingExtra: Int = 12,//行间距
        var paragraphSpacing: Int = 12,//段距
        var titleMode: Int = 0,//标题居中
        var paddingBottom: Int = 6,
        var paddingLeft: Int = 16,
        var paddingRight: Int = 16,
        var paddingTop: Int = 6,
        var headerPaddingBottom: Int = 0,
        var headerPaddingLeft: Int = 16,
        var headerPaddingRight: Int = 16,
        var headerPaddingTop: Int = 0,
        var footerPaddingBottom: Int = 6,
        var footerPaddingLeft: Int = 16,
        var footerPaddingRight: Int = 16,
        var footerPaddingTop: Int = 6
    ) {
        fun setBg(bgType: Int, bg: String) {
            if (AppConfig.isNightTheme) {
                bgTypeNight = bgType
                bgStrNight = bg
            } else {
                this.bgType = bgType
                bgStr = bg
            }
        }

        fun setTextColor(color: Int) {
            if (AppConfig.isNightTheme) {
                textColorNight = "#${color.hexString}"
            } else {
                textColor = "#${color.hexString}"
            }
            ChapterProvider.upStyle()
        }

        fun setStatusIconDark(isDark: Boolean) {
            if (AppConfig.isNightTheme) {
                darkStatusIconNight = isDark
            } else {
                darkStatusIcon = isDark
            }
        }

        fun statusIconDark(): Boolean {
            return if (AppConfig.isNightTheme) {
                darkStatusIconNight
            } else {
                darkStatusIcon
            }
        }

        fun textColor(): Int {
            return if (AppConfig.isNightTheme) Color.parseColor(textColorNight)
            else Color.parseColor(textColor)
        }

        fun bgStr(): String {
            return if (AppConfig.isNightTheme) bgStrNight
            else bgStr
        }

        fun bgType(): Int {
            return if (AppConfig.isNightTheme) bgTypeNight
            else bgType
        }

        fun bgDrawable(width: Int, height: Int): Drawable {
            var bgDrawable: Drawable? = null
            val resources = App.INSTANCE.resources
            try {
                bgDrawable = when (bgType()) {
                    0 -> ColorDrawable(Color.parseColor(bgStr()))
                    1 -> {
                        BitmapDrawable(
                            resources,
                            BitmapUtils.decodeAssetsBitmap(
                                App.INSTANCE,
                                "bg" + File.separator + bgStr(),
                                width,
                                height
                            )
                        )
                    }
                    else -> BitmapDrawable(
                        resources,
                        BitmapUtils.decodeBitmap(bgStr(), width, height)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bgDrawable ?: ColorDrawable(App.INSTANCE.getCompatColor(R.color.background))
        }
    }
}