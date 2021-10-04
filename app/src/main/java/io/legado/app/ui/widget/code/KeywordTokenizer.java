package io.legado.app.ui.widget.code;

import android.widget.MultiAutoCompleteTextView;

public class KeywordTokenizer implements MultiAutoCompleteTextView.Tokenizer {

    @Override
    public int findTokenStart(CharSequence charSequence, int cursor) {
        String sequenceStr = charSequence.toString();
        sequenceStr = sequenceStr.substring(0, cursor);

        int spaceIndex = sequenceStr.lastIndexOf(" ");
        int lineIndex = sequenceStr.lastIndexOf("\n");
        int bracketIndex = sequenceStr.lastIndexOf("(");

        int index = Math.max(0, Math.max(spaceIndex, Math.max(lineIndex, bracketIndex)));
        if (index == 0) return 0;
        return (index + 1 < charSequence.length()) ? index + 1 : index;
    }

    @Override
    public int findTokenEnd(CharSequence charSequence, int cursor) {
        return charSequence.length();
    }

    @Override
    public CharSequence terminateToken(CharSequence charSequence) {
        return charSequence;
    }

}
