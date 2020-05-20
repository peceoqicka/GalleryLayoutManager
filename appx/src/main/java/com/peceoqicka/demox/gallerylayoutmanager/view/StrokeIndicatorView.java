package com.peceoqicka.demox.gallerylayoutmanager.view;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.peceoqicka.demox.gallerylayoutmanager.R;

public class StrokeIndicatorView extends View {
    private int mNormalColor;
    private int mSelectedColor;
    private int mStrokeWidth;
    private int mStrokeHeight;
    private int mStrokeSelectedWidth;
    private int mStrokeSelectedHeight;
    private int mStrokeSpace;
    private int mItemCount = 0;
    private int mSelectedIndex = 0;

    private float mCurrentPercentage = 1f;
    private float mTargetPercentage = 0f;
    private int mWidthBias;
    private int mTargetIndex;
    private Paint mNormalPaint;
    private Paint mSelectedPaint;
    private Paint mBiasPaint;
    private ArgbEvaluator mArgbEvaluator;

    public StrokeIndicatorView(Context context) {
        this(context, null);
    }

    public StrokeIndicatorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public StrokeIndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getParams(context, attrs, defStyleAttr);
        initView();
    }

    private void getParams(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StrokeIndicatorView, defStyleAttr, 0);

        mNormalColor = typedArray.getColor(R.styleable.StrokeIndicatorView_siv_normalColor, Color.parseColor("#d9d9d9"));
        mSelectedColor = typedArray.getColor(R.styleable.StrokeIndicatorView_siv_selectedColor, Color.parseColor("#972f2d"));
        mStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.StrokeIndicatorView_siv_strokeWidth, 0);
        mStrokeHeight = typedArray.getDimensionPixelSize(R.styleable.StrokeIndicatorView_siv_strokeHeight, 0);
        mStrokeSelectedWidth = typedArray.getDimensionPixelSize(R.styleable.StrokeIndicatorView_siv_strokeSelectedWidth, 0);
        mStrokeSpace = typedArray.getDimensionPixelSize(R.styleable.StrokeIndicatorView_siv_strokeSpace, 0);

        typedArray.recycle();
    }

    private void initView() {
        mWidthBias = mStrokeSelectedWidth - mStrokeWidth;

        mNormalPaint = new Paint();
        mNormalPaint.setColor(mNormalColor);
        mNormalPaint.setStyle(Paint.Style.FILL);

        mSelectedPaint = new Paint();
        mSelectedPaint.setColor(mSelectedColor);
        mSelectedPaint.setStyle(Paint.Style.FILL);

        mBiasPaint = new Paint();
        mBiasPaint.setStyle(Paint.Style.FILL);

        mArgbEvaluator = new ArgbEvaluator();
    }

    /**
     * 设置Item的指示器个数，应对应于LayoutManager中的Item个数
     *
     * @param itemCount 指示器个数
     */
    public void setItemCount(int itemCount) {
        this.mItemCount = itemCount;
        if (mItemCount >= 0) {
            requestLayout();
        }
    }

    /**
     * 设置选中的位置，初始化后默认为0
     *
     * @param index 位置
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < mItemCount) {
            mSelectedIndex = index;
            mTargetIndex = -1;
            mCurrentPercentage = 1f;
            mTargetPercentage = 0f;
            postInvalidate();
        }
    }

    /**
     * 更新百分比以更新指示器的显示状态，即两个选项的中间态，如当前选中的是0，向目标项1滑动的过程中的状态
     * 总滑动距离即由当前位置滑动到目标位置所需滑动的总距离
     *
     * @param scrollingPercentage 百分比变化值，由LayoutManager滑动过程中dx占总滑动距离的百分比，带符号
     * @param fromPosition        当前位置
     * @param toPosition          目标位置
     */
    public void updateScrollingPercentage(float scrollingPercentage, int fromPosition, int toPosition) {
        int biasPosition = fromPosition - toPosition;
        if (biasPosition == 0) {
            return;
        }
        if (biasPosition == 1) {
            mCurrentPercentage += scrollingPercentage;
        } else if (biasPosition == -1) {
            mCurrentPercentage -= scrollingPercentage;
        } else if (biasPosition < -1) {
            //无限循环模式下向左滑动0->(itemCount-1)
            mCurrentPercentage += scrollingPercentage;
        } else {
            //无限循环模式下向右滑动(itemCount-1)->0
            mCurrentPercentage -= scrollingPercentage;
        }
        if (mCurrentPercentage > 1f) {
            mCurrentPercentage = 1f;
        } else if (mCurrentPercentage < 0f) {
            mCurrentPercentage = 0f;
        }

        mTargetPercentage = 1f - mCurrentPercentage;
        mSelectedIndex = fromPosition;
        mTargetIndex = toPosition;

        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int finalWidth;
        int finalHeight;

        int expectedWidth = mStrokeSelectedWidth + (mStrokeWidth + mStrokeSpace) * (mItemCount - 1);
        int expectedHeight = mStrokeHeight;
        if (widthMode == MeasureSpec.EXACTLY) {
            finalWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            finalWidth = Math.min(widthSize, expectedWidth);
        } else {
            finalWidth = expectedWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(heightSize, expectedHeight);
        } else {
            finalHeight = expectedHeight;
        }

        setMeasuredDimension(finalWidth, finalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float left = 0;
        for (int i = 0; i < mItemCount; i++) {
            float targetWidth;
            Paint targetPaint;
            float consumed;
            if (i == mSelectedIndex) {
                targetWidth = mStrokeWidth + mWidthBias * mCurrentPercentage;
                targetPaint = mSelectedPaint;
                int evaluateColor = (int) mArgbEvaluator.evaluate(mCurrentPercentage, mNormalColor, mSelectedColor);
                targetPaint.setColor(evaluateColor);
            } else if (i == mTargetIndex) {
                targetWidth = mStrokeWidth + mWidthBias * mTargetPercentage;
                targetPaint = mBiasPaint;
                targetPaint.setColor((int) mArgbEvaluator.evaluate(mTargetPercentage, mNormalColor, mSelectedColor));
            } else {
                targetWidth = mStrokeWidth;
                targetPaint = mNormalPaint;
            }
            consumed = targetWidth;
            if (i < mItemCount - 1) {
                consumed += mStrokeSpace;
            }

            canvas.drawRect(left, 0, left + targetWidth, mStrokeHeight, targetPaint);

            left += consumed;
        }

    }

    private void reset() {
        mWidthBias = mStrokeSelectedWidth - mStrokeWidth;
        mNormalPaint.setColor(mNormalColor);
        mSelectedPaint.setColor(mSelectedColor);
    }

    public void setParams(int normalWidth, int normalHeight, int selectionWidth, int selectionHeight, int space, int normalColor, int selectionColor) {
        this.mStrokeWidth = normalWidth;
        this.mStrokeHeight = normalHeight;
        this.mStrokeSelectedWidth = selectionWidth;
        this.mStrokeSelectedHeight = selectionHeight;
        this.mStrokeSpace = space;
        this.mNormalColor = normalColor;
        this.mSelectedColor = selectionColor;
        this.reset();

        System.out.println("normalWidth : " + normalWidth + " ; selectionWidth : " + selectionWidth);

        postInvalidate();
    }
}
