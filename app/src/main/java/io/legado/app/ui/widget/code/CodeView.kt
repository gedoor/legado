package io.legado.app.ui.widget.code;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class CodeView extends AppCompatMultiAutoCompleteTextView {

    private int tabWidth;
    private int tabWidthInCharacters;
    private int mUpdateDelayTime = 500;

    private boolean modified = true;
    private boolean highlightWhileTextChanging = true;

    private boolean hasErrors = false;
    private boolean mRemoveErrorsWhenTextChanged = true;

    private final Handler mUpdateHandler = new Handler();
    private Tokenizer mAutoCompleteTokenizer;
    private final float displayDensity = getResources().getDisplayMetrics().density;

    private static final Pattern PATTERN_LINE = Pattern.compile("(^.+$)+", Pattern.MULTILINE);
    private static final Pattern PATTERN_TRAILING_WHITE_SPACE = Pattern.compile("[\\t ]+$", Pattern.MULTILINE);

    private final SortedMap<Integer, Integer> mErrorHashSet = new TreeMap<>();
    private final Map<Pattern, Integer> mSyntaxPatternMap = new HashMap<>();
    private List<Character> mIndentCharacterList = Arrays.asList('{', '+', '-', '*', '/', '=');

    public CodeView(Context context) {
        super(context);
        initEditorView();
    }

    public CodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initEditorView();
    }

    public CodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initEditorView();
    }

    private void initEditorView() {
        if (mAutoCompleteTokenizer == null) {
            mAutoCompleteTokenizer = new KeywordTokenizer();
        }

        setTokenizer(mAutoCompleteTokenizer);

        setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        if (modified &&
                                end - start == 1 &&
                                start < source.length() &&
                                dstart < dest.length()) {
                            char c = source.charAt(start);

                            if (c == '\n') {
                                return autoIndent(source, dest, dstart, dend);
                            }
                        }
                        return source;
                    }
                }
        });
        addTextChangedListener(mEditorTextWatcher);
    }

    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        String indent = "";
        int istart = dstart - 1;

        boolean dataBefore = false;
        int pt = 0;

        for (; istart > -1; --istart) {
            char c = dest.charAt(istart);

            if (c == '\n') break;

            if (c != ' ' && c != '\t') {
                if (!dataBefore) {
                    if (mIndentCharacterList.contains(c)) --pt;
                    dataBefore = true;
                }

                if (c == '(') {
                    --pt;
                } else if (c == ')') {
                    ++pt;
                }
            }
        }

        if (istart > -1) {
            char charAtCursor = dest.charAt(dstart);
            int iend;

            for (iend = ++istart; iend < dend; ++iend) {
                char c = dest.charAt(iend);

                if (charAtCursor != '\n' &&
                        c == '/' &&
                        iend + 1 < dend &&
                        dest.charAt(iend) == c) {
                    iend += 2;
                    break;
                }

                if (c != ' ' && c != '\t') {
                    break;
                }
            }

            indent += dest.subSequence(istart, iend);
        }

        if (pt < 0) {
            indent += "\t";
        }

        return source + indent;
    }

    private void highlightSyntax(Editable editable) {
        if (mSyntaxPatternMap.isEmpty()) return;

        for (Pattern pattern : mSyntaxPatternMap.keySet()) {
            int color = mSyntaxPatternMap.get(pattern);
            for (Matcher m = pattern.matcher(editable); m.find(); ) {
                createForegroundColorSpan(editable, m, color);
            }
        }
    }

    private void highlightErrorLines(Editable editable) {
        if (mErrorHashSet.isEmpty()) return;
        int maxErrorLineValue = mErrorHashSet.lastKey();

        int lineNumber = 0;
        Matcher matcher = PATTERN_LINE.matcher(editable);
        while (matcher.find()) {
            if (mErrorHashSet.containsKey(lineNumber)) {
                int color = mErrorHashSet.get(lineNumber);
                createBackgroundColorSpan(editable, matcher, color);
            }
            lineNumber = lineNumber + 1;
            if (lineNumber > maxErrorLineValue) break;
        }
    }

    private void createForegroundColorSpan(Editable editable, Matcher matcher, @ColorInt int color) {
        editable.setSpan(new ForegroundColorSpan(color),
                matcher.start(), matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void createBackgroundColorSpan(Editable editable, Matcher matcher, @ColorInt int color) {
        editable.setSpan(new BackgroundColorSpan(color),
                matcher.start(), matcher.end(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private Editable highlight(Editable editable) {
        if (editable.length() == 0) return editable;

        try {
            clearSpans(editable);
            highlightErrorLines(editable);
            highlightSyntax(editable);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return editable;
    }

    private void highlightWithoutChange(Editable editable) {
        modified = false;
        highlight(editable);
        modified = true;
    }

    public void setTextHighlighted(CharSequence text) {
        if (text == null || text.length() == 0) return;

        cancelHighlighterRender();

        removeAllErrorLines();

        modified = false;
        setText(highlight(new SpannableStringBuilder(text)));
        modified = true;
    }

    public void setTabWidth(int characters) {
        if (tabWidthInCharacters == characters) return;
        tabWidthInCharacters = characters;
        tabWidth = Math.round(getPaint().measureText("m") * characters);
    }

    private void clearSpans(Editable editable) {
        int length = editable.length();
        ForegroundColorSpan[] foregroundSpans = editable.getSpans(
                0, length, ForegroundColorSpan.class);

        for (int i = foregroundSpans.length; i-- > 0; )
            editable.removeSpan(foregroundSpans[i]);

        BackgroundColorSpan[] backgroundSpans = editable.getSpans(
                0, length, BackgroundColorSpan.class);

        for (int i = backgroundSpans.length; i-- > 0; )
            editable.removeSpan(backgroundSpans[i]);
    }

    public void cancelHighlighterRender() {
        mUpdateHandler.removeCallbacks(mUpdateRunnable);
    }

    private void convertTabs(Editable editable, int start, int count) {
        if (tabWidth < 1) return;

        String s = editable.toString();

        for (int stop = start + count;
             (start = s.indexOf("\t", start)) > -1 && start < stop;
             ++start) {
            editable.setSpan(new TabWidthSpan(),
                    start,
                    start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public void setSyntaxPatternsMap(Map<Pattern, Integer> syntaxPatterns) {
        if (!mSyntaxPatternMap.isEmpty()) mSyntaxPatternMap.clear();
        mSyntaxPatternMap.putAll(syntaxPatterns);
    }

    public void addSyntaxPattern(Pattern pattern, @ColorInt int Color) {
        mSyntaxPatternMap.put(pattern, Color);
    }

    public void removeSyntaxPattern(Pattern pattern) {
        mSyntaxPatternMap.remove(pattern);
    }

    public int getSyntaxPatternsSize() {
        return mSyntaxPatternMap.size();
    }

    public void resetSyntaxPatternList() {
        mSyntaxPatternMap.clear();
    }

    public void setAutoIndentCharacterList(List<Character> characterList) {
        mIndentCharacterList = characterList;
    }

    public void clearAutoIndentCharacterList() {
        mIndentCharacterList.clear();
    }

    public List<Character> getAutoIndentCharacterList() {
        return mIndentCharacterList;
    }

    public void addErrorLine(int lineNum, int color) {
        mErrorHashSet.put(lineNum, color);
        hasErrors = true;
    }

    public void removeErrorLine(int lineNum) {
        mErrorHashSet.remove(lineNum);
        hasErrors = mErrorHashSet.size() > 0;
    }

    public void removeAllErrorLines() {
        mErrorHashSet.clear();
        hasErrors = false;
    }

    public int getErrorsSize() {
        return mErrorHashSet.size();
    }

    public String getTextWithoutTrailingSpace() {
        return PATTERN_TRAILING_WHITE_SPACE
                .matcher(getText())
                .replaceAll("");
    }

    public void setAutoCompleteTokenizer(Tokenizer tokenizer) {
        mAutoCompleteTokenizer = tokenizer;
    }

    public void setRemoveErrorsWhenTextChanged(boolean removeErrors) {
        mRemoveErrorsWhenTextChanged = removeErrors;
    }

    public void reHighlightSyntax() {
        highlightSyntax(getEditableText());
    }

    public void reHighlightErrors() {
        highlightErrorLines(getEditableText());
    }

    public boolean isHasError() {
        return hasErrors;
    }

    public void setUpdateDelayTime(int time) {
        mUpdateDelayTime = time;
    }

    public int getUpdateDelayTime() {
        return mUpdateDelayTime;
    }

    public void setHighlightWhileTextChanging(boolean updateWhileTextChanging) {
        this.highlightWhileTextChanging = updateWhileTextChanging;
    }

    @Override
    public void showDropDown() {
        int[] screenPoint = new int[2];
        getLocationOnScreen(screenPoint);

        final Rect displayFrame = new Rect();
        getWindowVisibleDisplayFrame(displayFrame);

        int position = getSelectionStart();
        Layout layout = getLayout();
        int line = layout.getLineForOffset(position);

        float verticalDistanceInDp = (750 + 140 * line) / displayDensity;
        setDropDownVerticalOffset((int) verticalDistanceInDp);

        float horizontalDistanceInDp = layout.getPrimaryHorizontal(position) / displayDensity;
        setDropDownHorizontalOffset((int) horizontalDistanceInDp);
        super.showDropDown();
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            Editable source = getText();
            highlightWithoutChange(source);
        }
    };

    private final TextWatcher mEditorTextWatcher = new TextWatcher() {

        private int start;
        private int count;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            this.start = start;
            this.count = count;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            if (!modified) return;

            if (highlightWhileTextChanging) {
                if (mSyntaxPatternMap.size() > 0) {
                    convertTabs(getEditableText(), start, count);
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime);
                }
            }

            if (mRemoveErrorsWhenTextChanged) removeAllErrorLines();
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (!highlightWhileTextChanging) {
                if (!modified) return;

                cancelHighlighterRender();

                if (mSyntaxPatternMap.size() > 0) {
                    convertTabs(getEditableText(), start, count);
                    mUpdateHandler.postDelayed(mUpdateRunnable, mUpdateDelayTime);
                }
            }
        }
    };

    private final class TabWidthSpan extends ReplacementSpan {

        @Override
        public int getSize(
                @NonNull Paint paint,
                CharSequence text,
                int start,
                int end,
                Paint.FontMetricsInt fm) {
            return tabWidth;
        }

        @Override
        public void draw(
                @NonNull Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                @NonNull Paint paint) {
        }
    }
}
