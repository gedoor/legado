@file:Suppress("unused")

package io.legado.app.utils


import io.legado.app.lib.theme.accentColor
import io.legado.app.ui.widget.code.CodeView
import splitties.init.appCtx
import java.util.regex.Pattern

val legadoPattern: Pattern = Pattern.compile("\\|\\||&&|%%|@js:|@Json:|@css:|@@|@XPath:")
val jsonPattern: Pattern = Pattern.compile("\"\\:|\"|\\{|\\}|\\[|\\]")
val jsPattern: Pattern = Pattern.compile("var|\\\\n")

fun CodeView.addLegadoPattern() {
    addSyntaxPattern(legadoPattern, appCtx.accentColor)
}

fun CodeView.addJsonPattern() {
    addSyntaxPattern(jsonPattern, appCtx.accentColor)
}

fun CodeView.addJsPattern() {
    addSyntaxPattern(jsPattern, appCtx.accentColor)
}