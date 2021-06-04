package io.legado.app.data.entities

import io.legado.app.R

class DrawLineEntity {

    // 第几章的划线
    var chapterIndex: Int = 0

    // 章节的第几个字符开始
    var startIndex: Int = 0

    // 本章的第几个字符结束
    var endIndex: Int = 0

    // 划线的样式
    var lineStyle: LineStyle = LineStyle.SHAPE

    // 划线颜色
    var lineColor: LineColor = LineColor.BLUE

    enum class LineColor {
        BLUE,
        ORANGE,
        YELLOW,
        GREEN
    }

    enum class LineStyle {
        STEEP, // 直线
        WAVE,  // 波浪线
        SHAPE, // 选中样式
    }


    fun getLineColor() = when (lineColor) {
        LineColor.BLUE -> R.color.md_blue_200
        LineColor.GREEN -> R.color.md_green_600
        LineColor.ORANGE -> R.color.md_orange_600
        LineColor.YELLOW -> R.color.md_yellow_600
    }
}