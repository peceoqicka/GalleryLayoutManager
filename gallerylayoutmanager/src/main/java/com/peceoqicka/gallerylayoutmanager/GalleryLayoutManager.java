package com.peceoqicka.gallerylayoutmanager;

import android.graphics.PointF;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * 支持无限循环的水平LayoutManager
 * <p>
 * 如有使用{@link SnapHelper}的需要, 推荐使用{@link GallerySnapHelper}
 * 调用{@link Builder#setDefaultSnapHelper()}, 或在构造方法中传入
 *
 * @author peceoqicka
 */
public class GalleryLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({POSITION_NONE, POSITION_CENTER, POSITION_START, POSITION_END, POSITION_CUSTOMIZED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TransformPosition {
    }

    public static final int POSITION_NONE = 0;
    public static final int POSITION_CENTER = 1;
    public static final int POSITION_START = 2;
    public static final int POSITION_END = 3;
    public static final int POSITION_CUSTOMIZED = 4;

    private ViewHelper mViewHelper;
    private SnapHelper mSnapHelper;
    private LayoutState mLayoutState;
    private AnchorInfo mAnchorInfo = new AnchorInfo();

    private int mTransformPosition;
    private boolean mIsInfinityModeEnabled = true;
    private boolean mIsInfinityModeOpened;
    private boolean mShouldLayoutCenter;
    private boolean mShouldLayoutCenterInFirstLayout;
    private float mCenterScaleX, mCenterScaleY;

    private int mCurrentPosition = 0;
    private int mLastTargetPosition = Integer.MIN_VALUE;
    private int mPendingScrollPosition = RecyclerView.NO_POSITION;
    private int mPendingScrollOffset = Integer.MIN_VALUE;
    private int mCenterX = 0;
    private int mLeftCenterX = Integer.MIN_VALUE;//在居中布局模式下的，居中Item的左边Item的中心点坐标
    private int mRightCenterX = Integer.MIN_VALUE;//在居中布局模式下的，居中Item的右边Item的中心点坐标
    private float mAXLeft = 1f, mBXLeft = 1f, mAYLeft = 1f, mBYLeft = 1f;
    private float mAXRight = 1f, mBXRight = 1f, mAYRight = 1f, mBYRight = 1f;

    private OnScrollListener mOnScrollListener = null;

    public GalleryLayoutManager(boolean infinityMode) {
        this(null, infinityMode);
    }

    public GalleryLayoutManager(SnapHelper snapHelper, boolean infinityMode) {
        this(snapHelper, infinityMode, POSITION_NONE);
    }

    public GalleryLayoutManager(SnapHelper snapHelper, boolean infinityMode, @TransformPosition int transformPosition) {
        this(snapHelper, infinityMode, transformPosition, null, null, false);
    }

    public GalleryLayoutManager(SnapHelper snapHelper, boolean infinityMode, @TransformPosition int transformPosition, float[] centerScale, int[] customizedTransformPosition, boolean shouldLayoutCenter) {
        this.mSnapHelper = snapHelper;
        this.mIsInfinityModeOpened = infinityMode;
        this.mTransformPosition = transformPosition;
        this.mShouldLayoutCenter = shouldLayoutCenter;

        this.mViewHelper = new ViewHelper(this);

        if (mShouldLayoutCenter) {
            mShouldLayoutCenterInFirstLayout = true;
        }

        if (centerScale != null && centerScale.length >= 2) {
            this.mCenterScaleX = centerScale[0];
            this.mCenterScaleY = centerScale[1];
        }

        if (transformPosition == POSITION_CUSTOMIZED && customizedTransformPosition != null && customizedTransformPosition.length >= 3) {
            mCenterX = customizedTransformPosition[0];
            mLeftCenterX = customizedTransformPosition[1];
            mRightCenterX = customizedTransformPosition[2];
        }
        if (this.mSnapHelper != null && this.mSnapHelper instanceof GallerySnapHelper) {
            ((GallerySnapHelper) this.mSnapHelper).attachToLayoutManager(this);
        }
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.mOnScrollListener = onScrollListener;
    }

    private void ensureLayoutState() {
        if (mLayoutState == null) {
            mLayoutState = new LayoutState();
        }
    }

    public boolean isInfinityMode() {
        return mIsInfinityModeEnabled && mIsInfinityModeOpened;
    }

    public int getTransformPosition() {
        return mTransformPosition;
    }

    private int getCenterX() {
        if (mTransformPosition == POSITION_NONE) {
            return getWidth() / 2;
        }
        return mCenterX;
    }

    private ViewHelper getViewHelper() {
        return mViewHelper;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean supportsPredictiveItemAnimations() {
        return true;
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean isAutoMeasureEnabled() {
        return true;
    }

    private boolean ensureInfinityModeAvailable(RecyclerView.State state) {
        final int childCount = getChildCount();
        if (childCount == state.getItemCount()) {
            View firstChild = getChildAt(0);
            View lastChild = getChildAt(getChildCount() - 1);
            if (firstChild == null || lastChild == null) {
                return false;
            }
            return mViewHelper.getDecoratedStart(firstChild) < 0 || mViewHelper.getDecoratedEnd(lastChild) > getWidth();
        }
        return childCount < state.getItemCount();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //System.out.println("-------------onLayoutChildren[PreLayout : " + state.isPreLayout() + "]----------------");
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        ensureLayoutState();
        mLayoutState.mShouldRecycle = false;
        mAnchorInfo.reset();
        updateAnchorInfoForLayout(state);
        mLayoutState.mLayoutDirection = mLayoutState.mLastScrollDelta >= 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;

        detachAndScrapAttachedViews(recycler);
        int startOffset, endOffset;
        if (mAnchorInfo.mLayoutFromEnd) {
            //System.out.println("layoutFromEnd");
            //从右往左布局
            updateLayoutStateToFillStart(mAnchorInfo.mPosition, mAnchorInfo.mCoordinate);
            endOffset = mLayoutState.mOffset;
            fill(recycler, state);
            startOffset = mLayoutState.mOffset;
            if (mShouldLayoutCenterInFirstLayout && isInfinityMode()) {
                int offset = offsetChildrenToLayoutCenter();
                if (offset != Integer.MIN_VALUE) {
                    startOffset += offset;
                    endOffset += offset;
                    mShouldLayoutCenterInFirstLayout = false;
                }
            }
            if (endOffset < getWidth() && !isInfinityMode()) {
                updateLayoutStateToFillEnd(mAnchorInfo.mPosition, startOffset);
                fill(recycler, state);
                endOffset = mLayoutState.mOffset;
            }
        } else {
            //System.out.println("layoutFromStart");
            updateLayoutStateToFillEnd(mAnchorInfo.mPosition, mAnchorInfo.mCoordinate);
            startOffset = mLayoutState.mOffset;
            fill(recycler, state);
            endOffset = mLayoutState.mOffset;
            if (mShouldLayoutCenterInFirstLayout && isInfinityMode()) {
                int offset = offsetChildrenToLayoutCenter();
                if (offset != Integer.MIN_VALUE) {
                    startOffset += offset;
                    endOffset += offset;
                    mShouldLayoutCenterInFirstLayout = false;
                }
            }
            if (startOffset > 0 && !isInfinityMode()) {
                updateLayoutStateToFillStart(mAnchorInfo.mPosition, startOffset);
                fill(recycler, state);
                startOffset = mLayoutState.mOffset;
            }
        }

        if (isInfinityMode()) {
            View firstChild = getChildAt(0);
            View lastChild = getChildAt(getChildCount() - 1);
            if (firstChild == null || lastChild == null) return;
            if (getPosition(firstChild) == 0 && mViewHelper.getDecoratedStart(firstChild) > 0) {
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
                updateLayoutStateInfinity(mViewHelper.getDecoratedStart(firstChild), state);
                fill(recycler, state);
                startOffset = mLayoutState.mOffset;
            } else if (getPosition(lastChild) == state.getItemCount() - 1 && mViewHelper.getDecoratedEnd(lastChild) < getWidth()) {
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
                updateLayoutStateInfinity(getWidth() - mViewHelper.getDecoratedEnd(lastChild), state);
                fill(recycler, state);
                endOffset = mLayoutState.mOffset;
            }
            applyScaleForLayoutViews();
        }

        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset);

        if (mLayoutState.mShouldCheckInfinityScroll) {
            mIsInfinityModeEnabled = ensureInfinityModeAvailable(state);
            mLayoutState.mShouldCheckInfinityScroll = false;
        }
        if (mLayoutState.mShouldCheckTransformParams) {
            prepareToTransform();
            mLayoutState.mShouldCheckTransformParams = false;
        }
        applyScaleForLayoutViews();
        //System.out.println("onLayoutChildren[END] -> childCount :" + getChildCount());
        //System.out.println("--------------------------------------------------------");
    }

    private int offsetChildrenToLayoutCenter() {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return Integer.MIN_VALUE;
        }
        View headChild = getChildAt(0);
        if (headChild == null) return Integer.MIN_VALUE;
        int left = mViewHelper.getDecoratedStart(headChild);
        int targetOffset = (getWidth() - mViewHelper.getDecoratedMeasuredWidth(headChild)) / 2;
        offsetChildrenHorizontal(targetOffset - left);
        return targetOffset - left;
    }

    private void prepareToTransform() {
        final int childCount = getChildCount();
        if (childCount == 0 || mTransformPosition == POSITION_NONE) {
            return;
        }
        switch (mTransformPosition) {
            case POSITION_CENTER:
                calculateTransformRangeCenter(childCount);
                break;
            case POSITION_START:
                calculateTransformRangeStart(childCount);
                break;
            case POSITION_END:
                calculateTransformRangeEnd(childCount);
                break;
        }

        if (mCenterScaleX != 1f || mCenterScaleY != 1f) {
            calculateScaleParams();
        }
        //Maybe add alpha change later
    }

    private void calculateTransformRangeCenter(int childCount) {
        int centerIndex = childCount % 2 == 0 ? (childCount / 2 - 1) : ((childCount - 1) / 2);
        int leftIndex = centerIndex - 1;
        int rightIndex = centerIndex + 1;
        calculateTransformRange(childCount, centerIndex, leftIndex, rightIndex);
    }

    private void calculateTransformRangeStart(int childCount) {
        int centerIndex = 0;
        int rightIndex = 1;
        int leftIndex = -1;
        calculateTransformRange(childCount, centerIndex, leftIndex, rightIndex);
    }

    private void calculateTransformRangeEnd(int childCount) {
        int centerIndex = childCount - 1;
        int leftIndex = centerIndex - 1;
        int rightIndex = centerIndex + 1;
        calculateTransformRange(childCount, centerIndex, leftIndex, rightIndex);
        View centerChild = getChildAt(centerIndex);
        if (centerChild == null) return;
        int requiredCenterLocation = getWidth() - mViewHelper.getDecoratedMeasuredWidth(centerChild) / 2;
        int currentCenterLocation = mViewHelper.getDecoratedCenterHorizontal(centerChild);
        int distance = requiredCenterLocation - currentCenterLocation;
        mCenterX += distance;
        mLeftCenterX += distance;
        mRightCenterX += distance;
    }

    private void calculateTransformRange(int childCount, int centerIndex, int leftIndex, int rightIndex) {
        View centerChild = getChildAt(centerIndex);
        if (centerChild == null) return;
        mCenterX = mViewHelper.getDecoratedCenterHorizontal(centerChild);
        if (leftIndex < 0 && rightIndex >= childCount) {
            mLeftCenterX = mCenterX - mViewHelper.getDecoratedMeasuredWidth(centerChild);
            mRightCenterX = mCenterX + mViewHelper.getDecoratedMeasuredWidth(centerChild);
        } else if (leftIndex < 0) {
            View rightChild = getChildAt(rightIndex);
            mRightCenterX = mViewHelper.getDecoratedCenterHorizontal(rightChild);
            mLeftCenterX = mCenterX - (mRightCenterX - mCenterX);
        } else if (rightIndex >= childCount) {
            View leftChild = getChildAt(leftIndex);
            mLeftCenterX = mViewHelper.getDecoratedCenterHorizontal(leftChild);
            mRightCenterX = mCenterX + (mCenterX - mLeftCenterX);
        } else {
            View leftChild = getChildAt(leftIndex);
            View rightChild = getChildAt(rightIndex);
            mLeftCenterX = mViewHelper.getDecoratedCenterHorizontal(leftChild);
            mRightCenterX = mViewHelper.getDecoratedCenterHorizontal(rightChild);
        }
    }

    /**
     * 计算缩放方程所需的参数
     */
    private void calculateScaleParams() {
        if (mCenterX <= mLeftCenterX || mCenterX >= mRightCenterX) {
            return;
        }
        mAXLeft = (mCenterScaleX - 1f) / (mCenterX - mLeftCenterX);
        mBXLeft = 1f - mLeftCenterX * mAXLeft;

        mAXRight = (mCenterScaleX - 1f) / (mCenterX - mRightCenterX);
        mBXRight = 1f - mRightCenterX * mAXRight;

        mAYLeft = (mCenterScaleY - 1f) / (mCenterX - mLeftCenterX);
        mBYLeft = 1f - mLeftCenterX * mAYLeft;

        mAYRight = (mCenterScaleY - 1f) / (mCenterX - mRightCenterX);
        mBYRight = 1f - mRightCenterX * mAYRight;
    }

    private float getScaleXByLocation(int centerX) {
        float scale = 1f;
        if (centerX >= mLeftCenterX && centerX < mCenterX) {
            scale = mAXLeft * centerX + mBXLeft;
        } else if (centerX > mCenterX && centerX <= mRightCenterX) {
            scale = mAXRight * centerX + mBXRight;
        } else if (centerX == mCenterX) {
            scale = mCenterScaleX;
        }
        return scale;
    }

    private float getScaleYByLocation(int centerX) {
        float scale = 1f;
        if (centerX >= mLeftCenterX && centerX < mCenterX) {
            scale = mAYLeft * centerX + mBYLeft;
        } else if (centerX > mCenterX && centerX <= mRightCenterX) {
            scale = mAYRight * centerX + mBYRight;
        } else if (centerX == mCenterX) {
            scale = mCenterScaleY;
        }
        return scale;
    }

    private void applyScaleForLayoutViews() {
        if (getChildCount() == 0 || (mCenterScaleX == 1f && mCenterScaleY == 1f) || mLeftCenterX == Integer.MIN_VALUE || mRightCenterX == Integer.MIN_VALUE) {
            return;
        }
        if (mCenterX <= mLeftCenterX || mCenterX >= mRightCenterX) {
            return;
        }
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view == null) continue;
            int measuredWidth = mViewHelper.getDecoratedMeasuredWidth(view);
            int left = mViewHelper.getDecoratedStart(view);
            int centerX = left + measuredWidth / 2;
            if (mCenterScaleX != 1f) {
                view.setScaleX(getScaleXByLocation(centerX));
            }
            if (mCenterScaleY != 1f) {
                view.setScaleY(getScaleYByLocation(centerX));
            }
        }
    }

    private void updateLayoutStateToFillStart(int itemPosition, int offset) {
        mLayoutState.mAvailable = offset;
        mLayoutState.mPosition = itemPosition;
        mLayoutState.mOffset = offset;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
    }

    private void updateLayoutStateToFillEnd(int itemPosition, int offset) {
        mLayoutState.mAvailable = getWidth() - offset;
        mLayoutState.mPosition = itemPosition;
        mLayoutState.mOffset = offset;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
    }

    private void updateAnchorInfoForLayout(RecyclerView.State state) {
        if (updateAnchorFromPendingData(state)) {
            return;
        }
        if (updateAnchorFromChildren(state)) {
            return;
        }
        mAnchorInfo.mCoordinate = 0;
        mAnchorInfo.mPosition = 0;
    }

    /**
     * 用于在调用{@link RecyclerView#scrollToPosition(int)}时正确的布局
     *
     * @param state {@link RecyclerView.State}
     * @return 当有上述方法被触发时会返回true, 默认为false
     */
    private boolean updateAnchorFromPendingData(RecyclerView.State state) {
        if (state.isPreLayout() || mPendingScrollPosition == RecyclerView.NO_POSITION) {
            return false;
        }
        mAnchorInfo.mPosition = mPendingScrollPosition;
        if (mPendingScrollOffset == Integer.MIN_VALUE) {
            View child = findViewByPosition(mPendingScrollPosition);
            if (child != null) {
                final int startGap = mViewHelper.getDecoratedStart(child);
                if (startGap < 0) {
                    mAnchorInfo.mCoordinate = 0;
                    return true;
                }
                final int endGap = mViewHelper.getDecoratedEnd(child) - getWidth();
                if (endGap > 0) {
                    mAnchorInfo.mCoordinate = getWidth();
                    mAnchorInfo.mLayoutFromEnd = true;
                    return true;
                }
                return false;
            } else {
                int positionToEnd = Math.abs(mAnchorInfo.mPosition - (state.getItemCount() - 1));
                if (positionToEnd < mAnchorInfo.mPosition || mAnchorInfo.mPosition == state.getItemCount() - 1) {
                    mAnchorInfo.mLayoutFromEnd = true;
                    mAnchorInfo.mCoordinate = getWidth();
                } else {
                    mAnchorInfo.mCoordinate = 0;
                }
                return true;
            }
        }
        mAnchorInfo.mCoordinate = mPendingScrollOffset;
        return true;
    }

    private boolean updateAnchorFromChildren(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return false;
        }
        View view = findReferenceChild(state);
        if (view != null) {
            mAnchorInfo.assign(mViewHelper.getDecoratedStart(view), getPosition(view));
            if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
                final boolean noVisible = mViewHelper.getDecoratedStart(view) >= getWidth() || mViewHelper.getDecoratedEnd(view) < 0;
                if (noVisible) {
                    mAnchorInfo.mCoordinate = 0;
                }
            }
            return true;
        }
        return false;
    }

    private View findReferenceChild(RecyclerView.State state) {
        View invalidMatch = null;
        View outOfBoundsMatch = null;
        int boundsStart = 0;
        int boundsEnd = getWidth();
        for (int i = 0; i < state.getItemCount(); i++) {
            View view = getChildAt(i);
            if (view == null) continue;
            int position = getPosition(view);
            if (position >= 0 && position < state.getItemCount()) {
                if (((RecyclerView.LayoutParams) view.getLayoutParams()).isItemRemoved()) {
                    if (invalidMatch == null) {
                        invalidMatch = view;//已经移除的View，非优先选择
                    }
                } else if (mViewHelper.getDecoratedStart(view) >= boundsEnd || mViewHelper.getDecoratedEnd(view) < boundsStart) {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view;//移动到不可见区域的View，非优先选择
                    }
                } else {
                    return view;
                }
            }
        }
        return outOfBoundsMatch != null ? outOfBoundsMatch : invalidMatch;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scrollBy(dx, recycler, state);
    }

    private int scrollBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        //System.out.println("scrollBy -> dx: " + dx);
        if (getChildCount() == 0 || dx == 0) {
            return 0;
        }
        //dx, fromPosition, toPosition
        ensureLayoutState();
        mLayoutState.mShouldRecycle = true;
        mLayoutState.mLayoutDirection = dx > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
        final int absDx = Math.abs(dx);
        updateLayoutState(absDx);
        final int scrollingOffsetX = mLayoutState.mScrollingOffsetX;
        int consumed = scrollingOffsetX + fill(recycler, state);
        if (consumed < 0) {
            return 0;
        } else if ((consumed == 0 || (scrollingOffsetX == consumed && (mLayoutState.mPosition == state.getItemCount() || mLayoutState.mPosition == -1))) && isInfinityMode()) {
            updateLayoutStateInfinity(absDx, state);
            fill(recycler, state);
            consumed = absDx;
        }
        final int scrolled = absDx > consumed ? consumed * mLayoutState.mLayoutDirection : dx;
        offsetChildrenHorizontal(-scrolled);
        calculateScrollingPercentage(dx, state);
        applyScaleForLayoutViews();
        mLayoutState.mLastScrollDelta = dx;
        return scrolled;
    }

    private void updateLayoutState(int absDx) {
        View startChild = getChildAt(0);
        View endChild = getChildAt(getChildCount() - 1);
        if (startChild == null || endChild == null) {
            return;
        }
        int scrollingOffset;
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mPosition = getPosition(startChild) + mLayoutState.mLayoutDirection;
            mLayoutState.mOffset = mViewHelper.getDecoratedStart(startChild);
            scrollingOffset = -mViewHelper.getDecoratedStart(startChild);
        } else {
            mLayoutState.mPosition = getPosition(endChild) + mLayoutState.mLayoutDirection;
            mLayoutState.mOffset = mViewHelper.getDecoratedEnd(endChild);
            scrollingOffset = mViewHelper.getDecoratedEnd(endChild) - getWidth();
        }

        mLayoutState.mAvailable = absDx - scrollingOffset;
        mLayoutState.mScrollingOffsetX = scrollingOffset;
    }

    private void updateLayoutStateInfinity(int available, RecyclerView.State state) {
        View startChild = getChildAt(0);
        View endChild = getChildAt(getChildCount() - 1);
        if (startChild == null || endChild == null) {
            return;
        }
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mPosition = state.getItemCount() - 1;
            mLayoutState.mOffset = mViewHelper.getDecoratedStart(startChild);
        } else {
            mLayoutState.mPosition = 0;
            mLayoutState.mOffset = mViewHelper.getDecoratedEnd(endChild);
        }

        mLayoutState.mAvailable = available;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
    }

    private int fill(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //System.out.println("-----------------------------------------");
        //System.out.println("fill -> width: " + getWidth() + " ; childCount : " + getChildCount() + " ; adapterItemCount : " + state.getItemCount());
        //System.out.println("scrollOffsetX : " + mLayoutState.mScrollingOffsetX);
        if (mLayoutState.mScrollingOffsetX != Integer.MIN_VALUE) {
            if (mLayoutState.mAvailable < 0) {
                mLayoutState.mScrollingOffsetX += mLayoutState.mAvailable;
                //这里的最终计算结果等于dx
            }
            recycleByLayoutState(recycler);
        }
        final int start = mLayoutState.mAvailable;
        int remainingSpace = start + mLayoutState.mExtraFillSpace;
        while (remainingSpace > 0 && mLayoutState.hasMore(state)) {
            final int consumed = layoutChunk(recycler);
            mLayoutState.mOffset += consumed * mLayoutState.mLayoutDirection;

            mLayoutState.mAvailable -= consumed;
            remainingSpace -= consumed;

            if (mLayoutState.mScrollingOffsetX != Integer.MIN_VALUE) {
                mLayoutState.mScrollingOffsetX += consumed;
                if (mLayoutState.mAvailable < 0) {
                    mLayoutState.mScrollingOffsetX += mLayoutState.mAvailable;
                }
                recycleByLayoutState(recycler);
            }
        }
        //System.out.println("after fill -> childCount : " + getChildCount());
        //System.out.println("--------------------------------------------");
        return start - mLayoutState.mAvailable;
    }

    private int layoutChunk(RecyclerView.Recycler recycler) {
        //System.out.println("layoutChunk");
        View view = mLayoutState.nextView(recycler);

        if (mLayoutState.mScrapList == null) {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                addView(view, 0);
            } else {
                addView(view);
            }
        } else {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                addDisappearingView(view, 0);
            } else {
                addDisappearingView(view);
            }
        }

        measureChildWithMargins(view, 0, 0);
        int measuredWidth = mViewHelper.getDecoratedMeasuredWidth(view);
        int measuredHeight = mViewHelper.getDecoratedMeasuredHeight(view);
        int left, right;
        int top = (getHeight() - measuredHeight) / 2;
        int bottom = top + measuredHeight;

        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            right = mLayoutState.mOffset;
            left = right - measuredWidth;
        } else {
            left = mLayoutState.mOffset;
            right = left + measuredWidth;
        }

        layoutDecoratedWithMargins(view, left, top, right, bottom);
        return measuredWidth;
    }

    private void recycleByLayoutState(RecyclerView.Recycler recycler) {
        if (!mLayoutState.mShouldRecycle) {
            return;
        }
        int threshold;
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            threshold = getWidth() - mLayoutState.mScrollingOffsetX;
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View child = getChildAt(i);
                if (child == null) continue;
                if (mViewHelper.getDecoratedStart(child) < threshold) {
                    recycleChildren(recycler, getChildCount() - 1, i);
                    return;
                }
            }
        } else {
            threshold = mLayoutState.mScrollingOffsetX;
            for (int i = 0; i < getChildCount() - 1; i++) {
                View child = getChildAt(i);
                if (child == null) continue;
                if (mViewHelper.getDecoratedEnd(child) > threshold) {
                    recycleChildren(recycler, 0, i);
                    return;
                }
            }
        }
    }

    private void recycleChildren(RecyclerView.Recycler recycler, int start, int end) {
        if (start == end) return;
        if (start < end) {
            for (int i = end - 1; i >= start; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        } else {
            for (int i = start; i > end; i--) {
                removeAndRecycleViewAt(i, recycler);
            }
        }
    }

    @Nullable
    @Override
    public View findViewByPosition(int position) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }
        final View firstView = getChildAt(0);
        if (firstView == null) return null;
        final int firstChild = getPosition(firstView);
        final int viewPosition = position - firstChild;
        if (viewPosition >= 0 && viewPosition < childCount) {
            View targetView = getChildAt(viewPosition);
            if (targetView == null) {
                return null;
            } else if (getPosition(targetView) == position) {
                return targetView;
            }
        }
        return super.findViewByPosition(position);
    }

    @Override
    public void scrollToPosition(int position) {
        mPendingScrollPosition = position;
        mPendingScrollOffset = Integer.MIN_VALUE;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext());
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        View firstChild = getChildAt(0);
        View lastChild = getChildAt(getChildCount() - 1);
        if (firstChild == null || lastChild == null) {
            return null;
        }
        int firstChildPosition = getPosition(firstChild);
        int direction = -1;
        if (isInfinityMode()) {
            int lastChildPosition = getPosition(lastChild);
            if (firstChildPosition > lastChildPosition && targetPosition >= firstChildPosition) {
                direction = 1;
            }
        } else {
            direction = targetPosition < firstChildPosition ? -1 : 1;
        }
        return new PointF(direction, 0);
    }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        ensureLayoutState();
        mLayoutState.mShouldCheckInfinityScroll = true;
        mLayoutState.mShouldCheckTransformParams = true;
        if (mShouldLayoutCenter) {
            mShouldLayoutCenterInFirstLayout = true;
        }
    }

    @Override
    public void onItemsAdded(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsAdded(recyclerView, positionStart, itemCount);
        mLayoutState.mShouldCheckInfinityScroll = true;
    }

    @Override
    public void onItemsRemoved(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
        mLayoutState.mShouldCheckInfinityScroll = true;
    }

    @Override
    public void onItemsChanged(@NonNull RecyclerView recyclerView) {
        super.onItemsChanged(recyclerView);
    }

    @Override
    public void onItemsMoved(@NonNull RecyclerView recyclerView, int from, int to, int itemCount) {
        super.onItemsMoved(recyclerView, from, to, itemCount);
    }

    @Override
    public void onItemsUpdated(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsUpdated(@NonNull RecyclerView recyclerView, int positionStart, int itemCount, @Nullable Object payload) {
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload);
    }

    private void layoutForPredictiveAnimations(RecyclerView.Recycler recycler, RecyclerView.State state, int startOffset, int endOffset) {
        if (!state.willRunPredictiveAnimations() || state.isPreLayout() || getChildCount() == 0 || !supportsPredictiveItemAnimations()) {
            return;
        }
        //System.out.println("layoutForPredictiveAnimations -> startOffset : " + startOffset + " ; endOffset : " + endOffset);
        //为了正确的执行动画，需要将由于数据产生变化而被暂且剥离并移入Recycler.scrapList中的View布局出来
        //这种情况通常由添加Item将原本的Item挤出可见区域，或者已有的Item扩展将其他的Item挤出可见区域导致
        int scrapExtraForStart = 0, scrapExtraForEnd = 0;
        final List<RecyclerView.ViewHolder> scrapList = recycler.getScrapList();
        final View firstChild = getChildAt(0);
        if (firstChild == null) return;
        final int firstChildPos = getPosition(firstChild);
        for (int i = 0; i < scrapList.size(); i++) {
            RecyclerView.ViewHolder scrap = scrapList.get(i);
            if (((RecyclerView.LayoutParams) scrap.itemView.getLayoutParams()).isItemRemoved()) {
                continue;
            }
            final int position = scrap.getLayoutPosition();
            final int direction = position < firstChildPos ? LayoutState.LAYOUT_START : LayoutState.LAYOUT_END;
            if (direction == LayoutState.LAYOUT_START) {
                scrapExtraForStart += mViewHelper.getDecoratedMeasuredWidth(scrap.itemView);
            } else {
                scrapExtraForEnd += mViewHelper.getDecoratedMeasuredWidth(scrap.itemView);
            }
        }

        mLayoutState.mScrapList = scrapList;
        if (scrapExtraForStart > 0) {
            View anchor = getChildAt(0);
            if (anchor == null) return;
            updateLayoutStateToFillStart(getPosition(anchor), startOffset);
            mLayoutState.mExtraFillSpace = scrapExtraForStart;
            mLayoutState.mAvailable = 0;
            mLayoutState.assignViewFromScrapList();
            fill(recycler, state);
        }

        if (scrapExtraForEnd > 0) {
            View anchor = getChildAt(getChildCount() - 1);
            if (anchor == null) return;
            updateLayoutStateToFillEnd(getPosition(anchor), endOffset);
            mLayoutState.mExtraFillSpace = scrapExtraForEnd;
            mLayoutState.mAvailable = 0;
            mLayoutState.assignViewFromScrapList();
            fill(recycler, state);
        }

        mLayoutState.mScrapList = null;
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        mPendingScrollPosition = RecyclerView.NO_POSITION;
        mPendingScrollOffset = Integer.MIN_VALUE;
        mAnchorInfo.reset();
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        if (mSnapHelper != null) {
            mSnapHelper.attachToRecyclerView(view);
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE:
                if (mSnapHelper != null) {
                    View view = mSnapHelper.findSnapView(this);
                    if (view != null) {
                        int adapterPosition = getPosition(view);
                        if (adapterPosition != mCurrentPosition) {
                            mLastTargetPosition = Integer.MIN_VALUE;
                        }
                        mCurrentPosition = adapterPosition;
                        if (mOnScrollListener != null) {
                            mOnScrollListener.onIdle(adapterPosition);
                        }
                    }
                }
                break;
            case RecyclerView.SCROLL_STATE_DRAGGING:
                //System.out.println("SCROLL_STATE_DRAGGING");
                if (mOnScrollListener != null) {
                    mOnScrollListener.onDragging();
                }
                break;
            case RecyclerView.SCROLL_STATE_SETTLING:
                //System.out.println("SCROLL_STATE_SETTLING");
                if (mOnScrollListener != null) {
                    mOnScrollListener.onSettling();
                }
                break;
        }
    }

    private void calculateScrollingPercentage(int dx, RecyclerView.State state) {
        if (mLeftCenterX == Integer.MIN_VALUE || mRightCenterX == Integer.MIN_VALUE) {
            return;
        }

        int estimatedPosition = dx > 0 ? (mCurrentPosition + 1) : (mCurrentPosition - 1);
        int targetPosition = mLastTargetPosition != Integer.MIN_VALUE ? mLastTargetPosition : estimatedPosition;
        //System.out.println("targetPosition[Before] : " + targetPosition);
        if (isInfinityMode()) {
            if (targetPosition == state.getItemCount()) {
                targetPosition = 0;
            } else if (targetPosition == -1) {
                targetPosition = state.getItemCount() - 1;
            }
        }
        //System.out.println("calculateScrollingPercentage -> mCurrentPosition : " + mCurrentPosition + " ; targetPosition : " + targetPosition);
        if (targetPosition < 0 || targetPosition > state.getItemCount() - 1) {
            return;
        }
        if (mOnScrollListener != null) {
            if (mLastTargetPosition == Integer.MIN_VALUE) {
                mLastTargetPosition = targetPosition;
                //System.out.println("assign mLastTargetPosition : " + mLastTargetPosition);
            }
            float distance = dx > 0 ? (mRightCenterX - getWidth() / 2f) : (getWidth() / 2f - mLeftCenterX);
            float scrollingPercentage = (dx * 1f) / distance;
            mOnScrollListener.onScrolling(scrollingPercentage, mCurrentPosition, targetPosition);
        }
    }

    public interface OnScrollListener {
        void onIdle(int snapViewPosition);

        void onScrolling(float scrollingPercentage, int fromPosition, int toPosition);

        void onDragging();

        void onSettling();
    }

    public static class Builder {
        int transformPosition = POSITION_NONE;
        boolean infinityMode = false;
        boolean layoutInCenter = false;
        SnapHelper snapHelper = null;
        float[] centerScale = new float[]{1, 1};
        int[] customizedTransformPosition = new int[]{0, 0, 0};
        OnScrollListener onScrollListener = null;

        public Builder setTransformPosition(@TransformPosition int transformPosition) {
            this.transformPosition = transformPosition;
            return this;
        }

        public Builder setInfinityMode(boolean infinityMode) {
            this.infinityMode = infinityMode;
            return this;
        }

        public Builder setLayoutInCenter(boolean layoutInCenter) {
            this.layoutInCenter = layoutInCenter;
            return this;
        }

        public Builder setSnapHelper(SnapHelper snapHelper) {
            this.snapHelper = snapHelper;
            return this;
        }

        public Builder setDefaultSnapHelper() {
            this.snapHelper = new GalleryLayoutManager.GallerySnapHelper();
            return this;
        }

        public Builder setCenterScale(float scaleX, float scaleY) {
            this.centerScale[0] = scaleX;
            this.centerScale[1] = scaleY;
            return this;
        }

        /**
         * 设置自定义变形范围需同时设置{@link Builder#transformPosition}为{@link GalleryLayoutManager#POSITION_CUSTOMIZED}
         * 才会生效，否则无效，且必须满足leftCenterX < centerX < rightCenterX
         *
         * @param centerX      中间坐标，即达到目标缩放值（或alpha值）的位置
         * @param leftCenterX  左边界，从左边界起到中间坐标为止，缩放值从1逐渐增大到目标缩放值
         * @param rightCenterX 右边界，与左边界相反
         * @return Builder
         */
        public Builder setCustomizedTransformPosition(int centerX, int leftCenterX, int rightCenterX) {
            this.customizedTransformPosition[0] = centerX;
            this.customizedTransformPosition[1] = leftCenterX;
            this.customizedTransformPosition[2] = rightCenterX;
            return this;
        }

        public Builder setOnScrollListener(OnScrollListener onScrollListener) {
            this.onScrollListener = onScrollListener;
            return this;
        }

        public GalleryLayoutManager build() {
            GalleryLayoutManager layoutManager = new GalleryLayoutManager(snapHelper, infinityMode, transformPosition, centerScale, customizedTransformPosition, layoutInCenter);
            layoutManager.setOnScrollListener(onScrollListener);
            return layoutManager;
        }
    }

    private static class GallerySnapHelper extends SnapHelper {
        private GalleryLayoutManager mLayout;

        public void attachToLayoutManager(GalleryLayoutManager layoutManager) {
            this.mLayout = layoutManager;
        }

        @Nullable
        @Override
        public int[] calculateDistanceToFinalSnap(@NonNull RecyclerView.LayoutManager layoutManager, @NonNull View view) {
            if (this.mLayout == null) {
                return new int[0];
            }
            int[] distance = new int[2];
            final int childCenter = mLayout.getViewHelper().getDecoratedCenterHorizontal(view);
            distance[0] = childCenter - mLayout.getCenterX();
            return distance;
        }

        @Nullable
        @Override
        public View findSnapView(RecyclerView.LayoutManager layoutManager) {
            if (this.mLayout == null || this.mLayout.getChildCount() == 0) {
                return null;
            }
            int childCount = this.mLayout.getChildCount();
            int centerX = mLayout.getCenterX();
            View closestChild = null;
            int absClosest = Integer.MAX_VALUE;
            for (int i = 0; i < childCount; i++) {
                final View child = this.mLayout.getChildAt(i);
                if (child == null) continue;
                int childCenter = this.mLayout.getViewHelper().getDecoratedCenterHorizontal(child);
                int absDistance = Math.abs(childCenter - centerX);
                if (absDistance < absClosest) {
                    absClosest = absDistance;
                    closestChild = child;
                }
            }

            return closestChild;
        }

        @Override
        public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int i, int i1) {
            return RecyclerView.NO_POSITION;
        }
    }

    private static class ViewHelper {
        RecyclerView.LayoutManager mLayoutManager;

        ViewHelper(RecyclerView.LayoutManager layoutManager) {
            this.mLayoutManager = layoutManager;
        }

        int getDecoratedMeasuredWidth(View view) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return mLayoutManager.getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
        }

        int getDecoratedMeasuredHeight(View view) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return mLayoutManager.getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
        }

        int getDecoratedStart(View view) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return mLayoutManager.getDecoratedLeft(view) - lp.leftMargin;
        }

        int getDecoratedEnd(View view) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
            return mLayoutManager.getDecoratedRight(view) + lp.rightMargin;
        }

        int getDecoratedCenterHorizontal(View view) {
            return getDecoratedStart(view) + getDecoratedMeasuredWidth(view) / 2;
        }
    }

    private static class AnchorInfo {
        int mCoordinate = Integer.MIN_VALUE;
        int mPosition = -1;
        int mExtraSpace = 0;
        boolean mLayoutFromEnd = false;

        void reset() {
            mCoordinate = Integer.MIN_VALUE;
            mPosition = -1;
            mExtraSpace = 0;
            mLayoutFromEnd = false;
        }

        void assign(int coordinate, int position) {
            this.mCoordinate = coordinate;
            this.mPosition = position;
        }
    }

    private static class LayoutState {
        static final int LAYOUT_START = -1;
        static final int LAYOUT_END = 1;

        int mOffset;
        int mPosition = 0;//默认序列的起始位置，用于从适配器中获取view
        int mScrollingOffsetX = Integer.MIN_VALUE;//滑动偏移值
        int mAvailable = 0;//剩余布局空间
        int mExtraFillSpace = 0;//额外的填充空间
        int mLayoutDirection = LAYOUT_END;//布局方向
        int mLastScrollDelta = 0;
        boolean mShouldRecycle = true;//是否需要在填充View前后进行回收操作
        boolean mShouldCheckInfinityScroll = true;
        boolean mShouldCheckTransformParams = true;
        List<RecyclerView.ViewHolder> mScrapList = null;//仅用于动画的布局阶段使用的

        boolean hasMore(RecyclerView.State state) {
            return mPosition >= 0 && mPosition < state.getItemCount();
        }

        View nextView(RecyclerView.Recycler recycler) {
            if (mScrapList != null) {
                return nextViewFromScrapList();
            }
            View view = recycler.getViewForPosition(mPosition);
            mPosition += mLayoutDirection;
            return view;
        }

        View nextViewFromScrapList() {
            for (int i = 0; i < mScrapList.size(); i++) {
                final View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (lp.isItemRemoved()) {
                    continue;
                }
                if (mPosition == lp.getViewLayoutPosition()) {
                    assignViewFromScrapList(view);
                    return view;
                }
            }
            return null;
        }

        void assignViewFromScrapList() {
            assignViewFromScrapList(null);
        }

        void assignViewFromScrapList(View ignore) {
            final View closest = nextViewInLimitedList(ignore);
            if (closest == null) {
                mPosition = RecyclerView.NO_POSITION;
            } else {
                mPosition = ((RecyclerView.LayoutParams) closest.getLayoutParams()).getViewLayoutPosition();
            }
        }

        View nextViewInLimitedList(View ignore) {
            if (mScrapList == null) return null;
            int size = mScrapList.size();
            View closest = null;
            int closestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                View view = mScrapList.get(i).itemView;
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                if (view == ignore || lp.isItemRemoved()) {
                    continue;
                }
                final int distance = lp.getViewLayoutPosition() - mPosition;
                if (distance < 0) {
                    continue;
                }
                if (distance < closestDistance) {
                    closest = view;
                    closestDistance = distance;
                    if (distance == 0) {
                        break;
                    }
                }
            }
            return closest;
        }
    }
}
