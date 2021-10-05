@file:Suppress("unused")

package io.legado.app.ui.widget.code

import android.graphics.Color
import io.legado.app.lib.theme.accentColor
import splitties.init.appCtx
import java.util.regex.Pattern

val legadoPattern: Pattern = Pattern.compile("\\|\\||&&|%%|@js:|@Json:|@css:|@@|@XPath:")
val jsonPattern: Pattern = Pattern.compile("\"[A-Za-z0-9]*?\"\\:|\"|\\{|\\}|\\[|\\]")
val wrapPattern: Pattern = Pattern.compile("\\\\n")
val jsPattern: Pattern = Pattern.compile("var|=")

fun CodeView.addLegadoPattern() {
    addSyntaxPattern(legadoPattern, appCtx.accentColor)
}

fun CodeView.addJsonPattern() {
    addSyntaxPattern(jsonPattern, appCtx.accentColor)
}

fun CodeView.addJsPattern() {
    addSyntaxPattern(wrapPattern, Color.DKGRAY)
    addSyntaxPattern(jsPattern, appCtx.accentColor)
}