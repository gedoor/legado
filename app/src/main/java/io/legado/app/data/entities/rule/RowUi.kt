package io.legado.app.data.entities.rule

data class RowUi(
    var name: String,
    var type: String = "text",
    var action: String? = null,
    var chars: Array<String>? = null,
    var default: String? = null,
    var viewName: String? = null,
    var style: FlexChildStyle? = null
) {

    @Suppress("ConstPropertyName")
    object Type {

        const val text = "text"
        const val password = "password"
        const val button = "button"
        const val toggle = "toggle"

    }

    fun style(): FlexChildStyle {
        return style ?: FlexChildStyle.defaultStyle
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RowUi) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (action != other.action) return false
        if (!chars.contentEquals(other.chars)) return false
        if (style != other.style) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + (chars?.contentHashCode() ?: 0)
        result = 31 * result + (style?.hashCode() ?: 0)
        return result
    }

}