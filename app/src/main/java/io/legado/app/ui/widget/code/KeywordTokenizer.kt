package io.legado.app.ui.widget.code

import android.widget.MultiAutoCompleteTextView
import kotlin.math.max

class KeywordTokenizer : MultiAutoCompleteTextView.Tokenizer {
    override fun findTokenStart(charSequence: CharSequence, cursor: Int): Int {
        var sequenceStr = charSequence.toString()
        sequenceStr = sequenceStr.substring(0, cursor)
        val spaceIndex = sequenceStr.lastIndexOf(" ")
        val lineIndex = sequenceStr.lastIndexOf("\n")
        val bracketIndex = sequenceStr.lastIndexOf("(")
        val index = max(0, max(spaceIndex, max(lineIndex, bracketIndex)))
        if (index == 0) return 0
        return if (index + 1 < charSequence.length) index + 1 else index
    }

    override fun findTokenEnd(charSequence: CharSequence, cursor: Int): Int {
        return charSequence.length
    }

    override fun terminateToken(charSequence: CharSequence): CharSequence {
        return charSequence
    }
}