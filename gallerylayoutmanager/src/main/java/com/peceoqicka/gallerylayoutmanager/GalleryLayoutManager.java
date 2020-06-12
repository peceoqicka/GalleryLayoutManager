package com.peceoqicka.gallerylayoutmanager;

import android.content.Context;
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({BASE_POSITION_CENTER, BASE_POSITION_START})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BasePosition {
    }

    //布局基准点
    public static final int BASE_POSITION_CENTER = 0;
    public static final int BASE_POSITION_START = 1;

    private ViewHelper mViewHelper;
    private SnapHelper mSnapHelper;
    private LayoutState mLayoutState;
    private AnchorInfo mAnchorInfo = new AnchorInfo();
    private LayoutChunkResult mLayoutChunkResult = new LayoutChunkResult();

    private int mTransformPosition;
    private int mBasePosition;
    private float mCenterScaleX, mCenterScaleY;

    private boolean mIsItemInsufficient = false;
    private int mExtraMargin = 0;
    private int mBaseCenterX = 0;
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

    public GalleryLayoutManager(SnapHelper snapHelper, int extraMargin, @BasePosition int basePosition, @TransformPosition int transformPosition, float[] centerScale, int[] customizedTransformPosition) {
        this.mSnapHelper = snapHelper;
        this.mExtraMargin = extraMargin;
        this.mBasePosition = basePosition;
        this.mTransformPosition = transformPosition;

        this.mViewHelper = new ViewHelper(this);

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

    public int getBasePosition() {
        return mBasePosition;
    }

    public int getTransformPosition() {
        return mTransformPosition;
    }

    private int getCenterX() {
        return mBaseCenterX;
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

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //System.out.println("-------------onLayoutChildren[PreLayout : " + state.isPreLayout() + "]----------------");
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        final int childCountBeforeLayout = getChildCount();
        ensureLayoutState();
        mLayoutState.mShouldRecycle = false;
        mAnchorInfo.reset();
        updateAnchorInfoForLayout(state);
        mLayoutState.mLayoutDirection = mLayoutState.mLastScrollDelta >= 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;

        detachAndScrapAttachedViews(recycler);
        int startOffset = 0, endOffset = 0;
        int startPosition, endPosition;
        if (mAnchorInfo.mLayoutFromEnd) {
            //System.out.println("layoutFromEnd");
            //从右往左布局
            updateLayoutStateToFillStart(mAnchorInfo.mPosition, mAnchorInfo.mBaseCoordinate, mAnchorInfo.mShouldAddCenterOffset);
            startPosition = mLayoutState.mPosition;
            endOffset = mLayoutState.mOffset;
            fill(recycler, state);
            endPosition = mLayoutState.mPosition;
            if (mLayoutState.mCalibratedOffset != Integer.MIN_VALUE) {
                endOffset = mLayoutState.mCalibratedOffset;
            }
            startOffset = mLayoutState.mOffset;
        } else {
            //System.out.println("layoutFromStart");
            updateLayoutStateToFillEnd(mAnchorInfo.mPosition, mAnchorInfo.mBaseCoordinate, mAnchorInfo.mShouldAddCenterOffset);
            startPosition = mLayoutState.mPosition;
            startOffset = mLayoutState.mOffset;
            fill(recycler, state);
            endPosition = mLayoutState.mPosition;
            if (mLayoutState.mCalibratedOffset != Integer.MIN_VALUE) {
                startOffset = mLayoutState.mCalibratedOffset;
            }
            endOffset = mLayoutState.mOffset;
        }

        final boolean hasLayoutAll = startPosition == 0 && endPosition == state.getItemCount();
        if (!hasLayoutAll && !mLayoutState.mForceToLayoutInfinitely) {
            //在已有布局好的View的情况下，从View中获取AnchorInfo时，可能出现的因为边界导致的布局中断，需要再次layout进行补充
            //如果第一次布局完成时发现所有的item已经被布局出来了，就不再重复布局
            while (endOffset < getWidth()) {
                //System.out.println("patchUpForEnd -> endOffset : " + endOffset);
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
                updateLayoutStateInfinity(state);
                fill(recycler, state);
                endOffset = mLayoutState.mOffset;
            }
            while (startOffset > 0) {
                //System.out.println("patchUpForStart -> startOffset : " + startOffset);
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
                updateLayoutStateInfinity(state);
                fill(recycler, state);
                startOffset = mLayoutState.mOffset;
            }
        } else {
            if (endOffset < getWidth()) {
                mLayoutState.mForceToLayoutInfinitely = true;
            }
            while (endOffset < getWidth()) {
                updateLayoutStateToFillEndInfinity(state);
                fill(recycler, state);
                endOffset = mLayoutState.mOffset;
            }
            if (startOffset > 0) {
                mLayoutState.mForceToLayoutInfinitely = true;
            }
            while (startOffset > 0) {
                updateLayoutStateToFillStartInfinity(state);
                fill(recycler, state);
                startOffset = mLayoutState.mOffset;
            }
        }

        //System.out.println("startOffset : " + startOffset + " ;  endOffset : " + endOffset);
        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset);

        if (mLayoutState.mShouldCheckTransformParams) {
            prepareToTransform();
            mLayoutState.mShouldCheckTransformParams = false;
        }
        applyScaleForLayoutViews();
        calculateBaseCenterX(childCountBeforeLayout, getChildCount());
        checkIfNotEnoughToScrollInfinitely(childCountBeforeLayout, getChildCount(), state.getItemCount());
        //System.out.println("onLayoutChildren[END] -> childCount :" + getChildCount());
        //System.out.println("--------------------------------------------------------");
    }

    private void calculateBaseCenterX(int childCountBeforeLayout, int childCountAfterLayout) {
        if (childCountBeforeLayout == 0 && childCountAfterLayout > 0) {
            View view = getChildAt(0);
            if (view != null) {
                if (mBasePosition == BASE_POSITION_CENTER) {
                    mBaseCenterX = mViewHelper.getCenter();
                } else if (mBasePosition == BASE_POSITION_START) {
                    mBaseCenterX = mViewHelper.getDecoratedCenterHorizontal(view);
                }
            }
        }
    }

    private void checkIfNotEnoughToScrollInfinitely(int childCountBeforeLayout, int childCountAfterLayout, int itemCount) {
        if (childCountBeforeLayout == 0 && childCountAfterLayout > 0 && itemCount <= 1) {
            mIsItemInsufficient = true;
        }
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
            //System.out.println("View[" + getPosition(view) + "] -> pivotX : " + view.getPivotX());
            //System.out.println("View[" + getPosition(view) + "] -> measuredWidth : " + measuredWidth);
            //System.out.println("View[" + getPosition(view) + "] -> actualWidth : " + view.getWidth());
            //System.out.println("View[" + getPosition(view) + "] -> centerX : " + centerX);
        }
    }

    private void updateLayoutStateToFillStartInfinity(RecyclerView.State state) {
        View firstChild = getChildAt(0);
        View lastChild = getChildAt(getChildCount() - 1);
        if (firstChild == null || lastChild == null) return;
        int left = mViewHelper.getDecoratedStart(firstChild);
        int firstChildPosition = getPosition(firstChild);
        //int lastChildPosition = getPosition(lastChild);

        //System.out.println("updateLayoutStateToFillStartInfinity");
        //System.out.println("firstChild[" + firstChildPosition + "], lastChild[" + lastChildPosition + "]");

        mLayoutState.mAvailable = left;
        //mLayoutState.mPosition = lastChildPosition;
        if (firstChildPosition == 0) {
            mLayoutState.mPosition = state.getItemCount() - 1;
        } else {
            mLayoutState.mPosition = firstChildPosition - 1;
        }
        mLayoutState.mOffset = left;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mShouldAddCenterOffset = false;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
        //System.out.println("mPosition : " + mLayoutState.mPosition + " ; mOffset : " + mLayoutState.mOffset + " ; mAvailable : " + mLayoutState.mAvailable);
    }

    private void updateLayoutStateToFillEndInfinity(RecyclerView.State state) {
        View firstChild = getChildAt(0);
        View lastChild = getChildAt(getChildCount() - 1);
        if (firstChild == null || lastChild == null) return;
        int right = mViewHelper.getDecoratedEnd(lastChild);
        int firstChildPosition = getPosition(firstChild);
        int lastChildPosition = getPosition(lastChild);
        //System.out.println("updateLayoutStateToFillEndInfinity");
        //System.out.println("firstChild[" + getPosition(firstChild) + "], lastChild[" + getPosition(lastChild) + "]");

        mLayoutState.mAvailable = getWidth() - right;
        if (lastChildPosition == state.getItemCount() - 1) {
            mLayoutState.mPosition = 0;
        } else {
            mLayoutState.mPosition = firstChildPosition;
        }
        mLayoutState.mOffset = right;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mShouldAddCenterOffset = false;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
        //System.out.println("mPosition : " + mLayoutState.mPosition + " ; mOffset : " + mLayoutState.mOffset + " ; mAvailable : " + mLayoutState.mAvailable);
    }

    private void updateLayoutStateToFillStart(int itemPosition, int offset, boolean shouldAddCenterOffset) {
        mLayoutState.mAvailable = offset;
        mLayoutState.mPosition = itemPosition;
        mLayoutState.mOffset = offset;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mShouldAddCenterOffset = shouldAddCenterOffset;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
        mLayoutState.mIsHeadItem = true;
    }

    private void updateLayoutStateToFillEnd(int itemPosition, int offset, boolean shouldAddCenterOffset) {
        mLayoutState.mAvailable = getWidth() - offset;
        mLayoutState.mPosition = itemPosition;
        mLayoutState.mOffset = offset;
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mShouldAddCenterOffset = shouldAddCenterOffset;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
        mLayoutState.mIsHeadItem = true;
    }

    private void updateAnchorInfoForLayout(RecyclerView.State state) {
        if (updateAnchorFromPendingData(state)) {
            return;
        }
        if (updateAnchorFromChildren(state)) {
            return;
        }
        //默认起点为中心
        mAnchorInfo.mBaseCoordinate = getWidth() / 2;
        mAnchorInfo.mShouldAddCenterOffset = true;
        if (mBasePosition == BASE_POSITION_START) {
            mAnchorInfo.mBaseCoordinate = 0;
            mAnchorInfo.mShouldAddCenterOffset = false;
        }
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
                if (mBasePosition == BASE_POSITION_CENTER) {
                    mAnchorInfo.mBaseCoordinate = getWidth() / 2;
                    mAnchorInfo.mShouldAddCenterOffset = true;
                    return true;
                }
                final int startGap = mViewHelper.getDecoratedStart(child);
                if (startGap < 0) {
                    mAnchorInfo.mBaseCoordinate = 0;
                    return true;
                }
                final int endGap = mViewHelper.getDecoratedEnd(child) - getWidth();
                if (endGap > 0) {
                    mAnchorInfo.mBaseCoordinate = getWidth();
                    mAnchorInfo.mLayoutFromEnd = true;
                    return true;
                }
                return false;
            } else {
                int positionToEnd = Math.abs(mAnchorInfo.mPosition - (state.getItemCount() - 1));
                if (mBasePosition == BASE_POSITION_CENTER) {
                    mAnchorInfo.mBaseCoordinate = getWidth() / 2;
                    mAnchorInfo.mShouldAddCenterOffset = true;
                } else if (positionToEnd < mAnchorInfo.mPosition || mAnchorInfo.mPosition == state.getItemCount() - 1) {
                    mAnchorInfo.mLayoutFromEnd = true;
                    mAnchorInfo.mBaseCoordinate = getWidth();
                } else {
                    mAnchorInfo.mBaseCoordinate = 0;
                }
                return true;
            }
        }
        mAnchorInfo.mBaseCoordinate = mPendingScrollOffset;
        return true;
    }

    private boolean updateAnchorFromChildren(RecyclerView.State state) {
        if (getChildCount() == 0) {
            return false;
        }
        View view = mLayoutState.mForceToLayoutInfinitely ? findReferenceChildClosestToCenter() : findReferenceChild(state);
        if (view != null) {
            mAnchorInfo.mShouldAddCenterOffset = false;
            mAnchorInfo.assign(mViewHelper.getDecoratedStart(view), getPosition(view));
            if (!state.isPreLayout() && supportsPredictiveItemAnimations()) {
                final boolean notVisible = mViewHelper.getDecoratedStart(view) >= getWidth() || mViewHelper.getDecoratedEnd(view) < 0;
                if (notVisible) {
                    mAnchorInfo.mBaseCoordinate = 0;
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
        for (int i = 0; i < getChildCount(); i++) {
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

    private View findReferenceChildClosestToCenter() {
        int childCount = getChildCount();
        int centerX = getCenterX();
        View closestChild = null;
        int absClosest = Integer.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child == null) continue;
            int childCenter = getViewHelper().getDecoratedCenterHorizontal(child);
            int absDistance = Math.abs(childCenter - centerX);
            if (absDistance < absClosest) {
                absClosest = absDistance;
                closestChild = child;
            }
        }

        return closestChild;
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
        } else if (consumed == 0 || (mLayoutState.mPosition == state.getItemCount() || mLayoutState.mPosition == -1)) {
            updateLayoutStateInfinityWhenScroll(absDx, state);
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

    private void updateLayoutStateInfinity(RecyclerView.State state) {
        View firstChild = getChildAt(0);
        View lastChild = getChildAt(getChildCount() - 1);
        if (firstChild == null || lastChild == null) return;
        int firstChildPosition = getPosition(firstChild);
        int lastChildPosition = getPosition(lastChild);
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mAvailable = mViewHelper.getDecoratedStart(firstChild);
            mLayoutState.mOffset = mViewHelper.getDecoratedStart(firstChild);
            mLayoutState.mPosition = firstChildPosition - 1;
            if (firstChildPosition == 0) {
                mLayoutState.mPosition = state.getItemCount() - 1;
            }
        } else {
            mLayoutState.mAvailable = getWidth() - mViewHelper.getDecoratedEnd(lastChild);
            mLayoutState.mOffset = mViewHelper.getDecoratedEnd(lastChild);
            mLayoutState.mPosition = lastChildPosition + 1;
            if (lastChildPosition == state.getItemCount() - 1) {
                mLayoutState.mPosition = 0;
            }
        }

        mLayoutState.mShouldAddCenterOffset = false;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
    }

    private void updateLayoutStateInfinityWhenScroll(int available, RecyclerView.State state) {
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

        mLayoutState.mShouldAddCenterOffset = false;
        mLayoutState.mAvailable = available;
        mLayoutState.mScrollingOffsetX = Integer.MIN_VALUE;
        mLayoutState.mCalibratedOffset = Integer.MIN_VALUE;
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
            mLayoutChunkResult.reset();
            layoutChunk(recycler, state);
            //System.out.println("layoutChunk -> childCount : " + getChildCount());
            if (mLayoutState.mShouldAddCenterOffset) {
                mLayoutState.mOffset -= mLayoutChunkResult.mOffset * mLayoutState.mLayoutDirection;
                mLayoutState.mCalibratedOffset = mLayoutState.mOffset;
                mLayoutState.mAvailable += mLayoutChunkResult.mOffset;
                remainingSpace += mLayoutChunkResult.mOffset;
                mLayoutState.mShouldAddCenterOffset = false;
            }
            final int totalConsumed = mLayoutChunkResult.mConsumed + mLayoutChunkResult.mExtraConsumed;
            mLayoutState.mOffset += totalConsumed * mLayoutState.mLayoutDirection;

            mLayoutState.mAvailable -= totalConsumed;
            remainingSpace -= totalConsumed;

            if (mLayoutState.mScrollingOffsetX != Integer.MIN_VALUE) {
                mLayoutState.mScrollingOffsetX += totalConsumed;
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

    private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state) {
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
        mLayoutChunkResult.mConsumed = measuredWidth;

        int offsetForLayoutCenter = 0;
        if (mBasePosition == BASE_POSITION_CENTER) {
            offsetForLayoutCenter = measuredWidth / 2;
        }
        mLayoutChunkResult.mOffset = offsetForLayoutCenter;
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            right = mLayoutState.mOffset;
            if (mLayoutState.mShouldAddCenterOffset) {
                right += offsetForLayoutCenter;
                mLayoutState.mIsHeadItem = false;
            } else {
                if (mLayoutState.mIsHeadItem) {
                    mLayoutState.mIsHeadItem = false;
                } else {
                    right -= mExtraMargin;
                    mLayoutChunkResult.mExtraConsumed = mExtraMargin;
                }
            }
            left = right - measuredWidth;
        } else {
            left = mLayoutState.mOffset;

            if (mLayoutState.mShouldAddCenterOffset) {
                left -= offsetForLayoutCenter;
                mLayoutState.mIsHeadItem = false;
            } else {
                if (mLayoutState.mIsHeadItem) {
                    mLayoutState.mIsHeadItem = false;
                } else {
                    left += mExtraMargin;
                    mLayoutChunkResult.mExtraConsumed = mExtraMargin;
                }
            }
            right = left + measuredWidth;
        }

        layoutDecoratedWithMargins(view, left, top, right, bottom);
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
        if (mIsItemInsufficient) return;
        mPendingScrollPosition = position;
        mPendingScrollOffset = Integer.MIN_VALUE;
        requestLayout();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (mIsItemInsufficient) return;
        GallerySmoothScroller smoothScroller = new GallerySmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
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
        int lastChildPosition = getPosition(lastChild);
        if (firstChildPosition > lastChildPosition && targetPosition >= firstChildPosition) {
            direction = 1;
        }

        return new PointF(direction, 0);
    }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        super.onAdapterChanged(oldAdapter, newAdapter);
        ensureLayoutState();
        mLayoutState.mShouldCheckTransformParams = true;
    }

    @Override
    public void onItemsAdded(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsAdded(recyclerView, positionStart, itemCount);
    }

    @Override
    public void onItemsRemoved(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        super.onItemsRemoved(recyclerView, positionStart, itemCount);
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
            updateLayoutStateToFillStart(getPosition(anchor), startOffset, false);
            mLayoutState.mExtraFillSpace = scrapExtraForStart;
            mLayoutState.mAvailable = 0;
            mLayoutState.assignViewFromScrapList();
            fill(recycler, state);
        }

        if (scrapExtraForEnd > 0) {
            View anchor = getChildAt(getChildCount() - 1);
            if (anchor == null) return;
            updateLayoutStateToFillEnd(getPosition(anchor), endOffset, false);
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
                    //System.out.println("SCROLL_STATE_IDLE -> snapViewPosition : ");
                    if (view != null) {
                        int adapterPosition = getPosition(view);
                        //System.out.println("[" + adapterPosition + "]");
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
        if (targetPosition == state.getItemCount()) {
            targetPosition = 0;
        } else if (targetPosition == -1) {
            targetPosition = state.getItemCount() - 1;
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

    public static class SimpleScrollListener implements OnScrollListener {

        @Override
        public void onIdle(int snapViewPosition) {
        }

        @Override
        public void onScrolling(float scrollingPercentage, int fromPosition, int toPosition) {
        }

        @Override
        public void onDragging() {
        }

        @Override
        public void onSettling() {
        }
    }

    public interface OnScrollListener {
        void onIdle(int snapViewPosition);

        void onScrolling(float scrollingPercentage, int fromPosition, int toPosition);

        void onDragging();

        void onSettling();
    }

    public static class Builder {
        int extraMargin = 0;
        /**
         * 布局基准点，默认为可视区域中心，即选中的Item在中心位置
         */
        int basePosition = BASE_POSITION_CENTER;
        int transformPosition = POSITION_NONE;
        SnapHelper snapHelper = null;
        float[] centerScale = new float[]{1, 1};
        int[] customizedTransformPosition = new int[]{0, 0, 0};
        OnScrollListener onScrollListener = null;

        public Builder setExtraMargin(int extraMargin) {
            this.extraMargin = extraMargin;
            return this;
        }

        public Builder setBasePosition(@BasePosition int basePosition) {
            this.basePosition = basePosition;
            return this;
        }

        public Builder setTransformPosition(@TransformPosition int transformPosition) {
            this.transformPosition = transformPosition;
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
            GalleryLayoutManager layoutManager = new GalleryLayoutManager(snapHelper, extraMargin, basePosition, transformPosition, centerScale, customizedTransformPosition);
            layoutManager.setOnScrollListener(onScrollListener);
            return layoutManager;
        }
    }

    private static class GallerySmoothScroller extends LinearSmoothScroller {

        GallerySmoothScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDxToMakeVisible(View view, int snapPreference) {
            final RecyclerView.LayoutManager layoutManager = getLayoutManager();
            if (layoutManager == null || !layoutManager.canScrollHorizontally() || !(layoutManager instanceof GalleryLayoutManager)) {
                return 0;
            }
            //System.out.println("calculateDxToMakeVisible -> snapPreference : " + snapPreference);
            GalleryLayoutManager layout = (GalleryLayoutManager) layoutManager;
            final int left = layout.getViewHelper().getDecoratedStart(view);
            final int right = layout.getViewHelper().getDecoratedEnd(view);
            final int start = layoutManager.getPaddingLeft();
            final int end = layoutManager.getWidth() - layoutManager.getPaddingRight();
            final int center = layout.getViewHelper().getCenter();
            final boolean shouldOffsetToCenter = layout.getBasePosition() == GalleryLayoutManager.BASE_POSITION_CENTER;
            return calculateDtToFit(left, right, center, start, end, snapPreference, shouldOffsetToCenter);
        }

        private int calculateDtToFit(int viewStart, int viewEnd, int boxCenter, int boxStart, int boxEnd, int
                snapPreference, boolean shouldOffsetToCenter) {
            final int viewWidthHalf = (viewEnd - viewStart) / 2;

            switch (snapPreference) {
                case SNAP_TO_START:
                    //System.out.println("SNAP_TO_START : ");
                    if (shouldOffsetToCenter) {
                        return boxCenter - viewStart + viewWidthHalf;
                    }
                    return boxStart - viewStart;
                case SNAP_TO_END:
                    //System.out.println("SNAP_TO_END : ");
                    if (shouldOffsetToCenter) {
                        return boxCenter - viewEnd + viewWidthHalf;
                    }
                    return boxEnd - viewEnd;
                case SNAP_TO_ANY:
                    //System.out.println("SNAP_TO_ANY -> shouldOffsetToCenter : " + shouldOffsetToCenter);
                    if (shouldOffsetToCenter) {
                        final int dtStartC = boxCenter - viewWidthHalf - viewStart;
                        if (dtStartC > 0) {
                            return dtStartC;
                        }
                        final int dtEndC = boxCenter + viewWidthHalf - viewEnd;
                        if (dtEndC < 0) {
                            return dtEndC;
                        }
                    }
                    final int dtStart = boxStart - viewStart;
                    if (dtStart > 0) {
                        return dtStart;
                    }
                    final int dtEnd = boxEnd - viewEnd;
                    if (dtEnd < 0) {
                        return dtEnd;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("snap preference should be one of the"
                            + " constants defined in SmoothScroller, starting with SNAP_");
            }
            return 0;
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
            //System.out.println("calculateDistanceToFinalSnap : Item[" + mLayout.getPosition(view) + "]");
            int[] distance = new int[2];
            final int childCenter = mLayout.getViewHelper().getDecoratedCenterHorizontal(view);
            //System.out.println("calculateDistanceToFinalSnap -> centerX : " + mLayout.getCenterX());
            distance[0] = childCenter - mLayout.getCenterX();
            //System.out.println("calculateDistanceToFinalSnap -> distanceX : " + distance[0]);
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
            //System.out.println("findSnapView -> centerX : " + centerX);
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
        public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
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

        int getCenter() {
            return mLayoutManager.getWidth() / 2;
        }
    }

    private static class AnchorInfo {
        int mBaseCoordinate = Integer.MIN_VALUE;
        int mPosition = -1;
        int mExtraSpace = 0;
        boolean mLayoutFromEnd = false;
        boolean mShouldAddCenterOffset = false;

        void reset() {
            mBaseCoordinate = Integer.MIN_VALUE;
            mPosition = -1;
            mExtraSpace = 0;
            mLayoutFromEnd = false;
            mShouldAddCenterOffset = false;
        }

        void assign(int coordinate, int position) {
            this.mBaseCoordinate = coordinate;
            this.mPosition = position;
        }
    }

    private static class LayoutChunkResult {
        int mConsumed;
        int mOffset;
        int mExtraConsumed;

        void reset() {
            mConsumed = 0;
            mOffset = 0;
            mExtraConsumed = 0;
        }
    }

    private static class LayoutState {
        static final int LAYOUT_START = -1;
        static final int LAYOUT_END = 1;

        int mOffset;
        int mCalibratedOffset = Integer.MIN_VALUE;
        int mPosition = 0;//默认序列的起始位置，用于从适配器中获取view
        int mScrollingOffsetX = Integer.MIN_VALUE;//滑动偏移值
        int mAvailable = 0;//剩余布局空间
        int mExtraFillSpace = 0;//额外的填充空间
        int mLayoutDirection = LAYOUT_END;//布局方向
        int mLastScrollDelta = 0;
        boolean mIsHeadItem = false;
        boolean mShouldAddCenterOffset = false;
        boolean mShouldRecycle = true;//是否需要在填充View前后进行回收操作
        boolean mShouldCheckTransformParams = true;
        boolean mForceToLayoutInfinitely = false;//在item数量不足以铺满RecyclerView可视区域时，强制填充重复Item以达到无限循环的效果
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
