package io.legado.app.data.entities.rule

data class RowUi(
    var name: String = "",
    var type: String = "text",
    var action: String? = null,
    var style: FlexChildStyle? = null
) {

    @Suppress("ConstPropertyName")
    object Type {

        const val text = "text"
        const val password = "password"
        const val button = "button"

    }

    fun style(): FlexChildStyle {
        return style ?: FlexChildStyle.defaultStyle
    }

}