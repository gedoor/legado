package io.legado.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import io.legado.app.R;
import androidx.annotation.Nullable;

public class ArcView extends View {
    private int mWidth;
    private int mHeight;
    /**
     * 弧形高度
     */
    private int mArcHeight;
    /**
     * 背景颜色
     */
    private int mBgColor;
    private Paint mPaint;
    private boolean mDirectionTop;
    private Context mContext;

    public ArcView(Context context) {
        this(context, null);
    }

    public ArcView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArcView);
        mArcHeight = typedArray.getDimensionPixelSize(R.styleable.ArcView_arcHeight, 0);
        mBgColor = typedArray.getColor(R.styleable.ArcView_bgColor, Color.parseColor("#303F9F"));
        mDirectionTop = typedArray.getBoolean(R.styleable.ArcView_arcDirectionTop, false);
        mContext = context;
        mPaint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mBgColor);

        if (mDirectionTop) {
            Rect rect = new Rect(0, mArcHeight, mWidth, mHeight);
            canvas.drawRect(rect, mPaint);


            Path path = new Path();
            path.moveTo(0, mArcHeight);
            path.quadTo(mWidth / 2, 0, mWidth,  mArcHeight);
            canvas.drawPath(path, mPaint);
        } else {
            Rect rect = new Rect(0, 0, mWidth, mHeight - mArcHeight);
            canvas.drawRect(rect, mPaint);


            Path path = new Path();
            path.moveTo(0, mHeight - mArcHeight);
            path.quadTo(mWidth / 2, mHeight, mWidth, mHeight - mArcHeight);
            canvas.drawPath(path, mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY) {
            mWidth = widthSize;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mHeight = heightSize;
        }
        setMeasuredDimension(mWidth, mHeight);
    }
}