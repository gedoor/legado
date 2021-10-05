@file:Suppress("unused")

package io.legado.app.ui.widget.code

import android.content.Context
import android.widget.ArrayAdapter
import io.legado.app.R
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.secondaryTextColor
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
    addSyntaxPattern(wrapPattern, appCtx.secondaryTextColor)
    addSyntaxPattern(jsPattern, appCtx.accentColor)
}

fun Context.arrayAdapter(keywords: Array<String>): ArrayAdapter<String> {
    return ArrayAdapter(this, R.layout.item_text, R.id.text_view, keywords)
}