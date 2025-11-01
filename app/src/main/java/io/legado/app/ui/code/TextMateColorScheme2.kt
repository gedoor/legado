package io.legado.app.ui.code

import androidx.core.graphics.toColorInt
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel

class TextMateColorScheme2(themeRegistry: ThemeRegistry, themeModel: ThemeModel) : TextMateColorScheme(themeRegistry, themeModel) {
    override fun applyDefault() {
        super.applyDefault()
        if (isDark)
            applyDarkThemeColors()
        else
            applyLightThemeColors()
    }

    private fun applyDarkThemeColors() {
        setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, "#60FFFFFF".toColorInt()) // 选中括号
        setColor(SCROLL_BAR_THUMB, "#FF27292A".toColorInt())
        setColor(SCROLL_BAR_THUMB_PRESSED, "#90D8D8D8".toColorInt()) // 滚动条反色
        setColor(LINE_NUMBER_PANEL_TEXT, "#80D8D8D8".toColorInt()) // 滚动条提示文本色
    }

    private fun applyLightThemeColors() {
        setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, "#60000000".toColorInt())
    }
}
