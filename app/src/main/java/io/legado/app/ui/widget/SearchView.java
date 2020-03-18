package io.legado.app.ui.widget;

import android.app.SearchableInfo;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.legado.app.R;

public class SearchView extends androidx.appcompat.widget.SearchView {
    private Drawable mSearchHintIcon = null;
    private TextView textView = null;

    public SearchView(Context context) {
        this(context, null);
    }

    public SearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        try {
            if (textView == null) {
                textView = (TextView) this.findViewById(androidx.appcompat.R.id.search_src_text);
                mSearchHintIcon = this.getContext().getDrawable(R.drawable.ic_search_hint);
                updateQueryHint();
            }
            // 改变字体
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            textView.setGravity(Gravity.CENTER_VERTICAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private CharSequence getDecoratedHint(CharSequence hintText) {
        // If the field is always expanded or we don't have a search hint icon,
        // then don't add the search icon to the hint.
        if (mSearchHintIcon == null) {
            return hintText;
        }

        final int textSize = (int) (textView.getTextSize() * 0.8);
        mSearchHintIcon.setBounds(0, 0, textSize, textSize);

        final SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.setSpan(new CenteredImageSpan(mSearchHintIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(hintText);
        return ssb;
    }

    private void updateQueryHint() {
        if (textView != null) {
            final CharSequence hint = getQueryHint();
            textView.setHint(getDecoratedHint(hint == null ? "" : hint));
        }
    }

    @Override
    public void setIconifiedByDefault(boolean iconified) {
        super.setIconifiedByDefault(iconified);
        updateQueryHint();
    }

    @Override
    public void setSearchableInfo(SearchableInfo searchable) {
        super.setSearchableInfo(searchable);
        if (searchable != null)
            updateQueryHint();
    }

    @Override
    public void setQueryHint(@Nullable CharSequence hint) {
        super.setQueryHint(hint);
        updateQueryHint();
    }
}

class CenteredImageSpan extends ImageSpan {

    public CenteredImageSpan(final Drawable drawable) {
        super(drawable);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        // image to draw
        Drawable b = getDrawable();
        // font metrics of text to be replaced
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int transY = (y + fm.descent + y + fm.ascent) / 2
                - b.getBounds().bottom / 2;

        canvas.save();
        canvas.translate(x, transY);
        b.draw(canvas);
        canvas.restore();
    }
}