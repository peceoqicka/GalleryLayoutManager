package com.peceoqicka.gallerylayoutmanager

import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.support.annotation.IntDef
import android.support.annotation.RestrictTo
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SnapHelper
import android.view.View
import android.view.ViewGroup
import kotlin.math.abs

/**
 * 支持无限循环的水平LayoutManager
 *
 * @author peceoqicka
 */
class GalleryLayoutManager(
    private val mSnapHelper: SnapHelper? = null,
    private val isForceToScrollToRight: Boolean = false,
    private val mItemSpace: Int = 0,
    @param:BasePosition val basePosition: Int = BASE_POSITION_CENTER,
    private val mViewTransformListener: ViewTransformListener? = null
) : RecyclerView.LayoutManager(), RecyclerView.SmoothScroller.ScrollVectorProvider {


    companion object {
        //布局基准点
        const val BASE_POSITION_CENTER = 0
        const val BASE_POSITION_START = 1

        fun create(block: Builder.() -> Unit): GalleryLayoutManager {
            return Builder().apply {
                block(this)
            }.build()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(BASE_POSITION_CENTER, BASE_POSITION_START)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class BasePosition

    private val viewHelper = ViewHelper(this)
    private val mLayoutState = LayoutState()
    private val mAnchorInfo = AnchorInfo()
    private val mLayoutChunkResult = LayoutChunkResult()
    private var mIsItemInsufficient = false
    private var mIsNeedToFixScrollingDirection = false
    private var mIsItemNotEnough = false
    private var mTransformRangeCalculated = false
    private var centerX = 0

    //private int mCurrentPosition = 0;
    //private int mLastTargetPosition = Integer.MIN_VALUE;
    private var mPendingScrollPosition = RecyclerView.NO_POSITION
    private var mPendingScrollOffset = Int.MIN_VALUE
    private var mOnScrollListener: OnScrollListener? = null

    var onScrollListener: OnScrollListener?
        set(value) {
            mOnScrollListener = value
        }
        get() = mOnScrollListener


    init {
        mSnapHelper?.let { helper ->
            if (helper is GallerySnapHelper) {
                helper.attachToLayoutManager(this)
            }
        }
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun supportsPredictiveItemAnimations(): Boolean {
        return true
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun isAutoMeasureEnabled(): Boolean {
        return true
    }

    fun isItemNotEnough(): Boolean {
        return mIsItemNotEnough
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        //System.out.println("-------------onLayoutChildren[PreLayout : " + state.isPreLayout() + "]----------------");
        if (state.itemCount == 0) {
            removeAndRecycleAllViews(recycler)
            return
        }
        val childCountBeforeLayout = childCount
        mLayoutState.mShouldRecycle = false
        mAnchorInfo.reset()
        updateAnchorInfoForLayout(state)
        mLayoutState.mFirstLayout = childCountBeforeLayout == 0
        mLayoutState.mLayoutDirection =
            if (mLayoutState.mLastScrollDelta >= 0) LayoutState.LAYOUT_END else LayoutState.LAYOUT_START
        detachAndScrapAttachedViews(recycler)
        var startOffset: Int
        var endOffset: Int
        val startPosition: Int
        val endPosition: Int
        if (mAnchorInfo.mLayoutFromEnd) {
            //System.out.println("layoutFromEnd");
            //从右往左布局
            updateLayoutStateToFillStart(
                mAnchorInfo.mPosition,
                mAnchorInfo.mBaseCoordinate,
                mAnchorInfo.mShouldAddCenterOffset
            )
            startPosition = mLayoutState.mPosition
            endOffset = mLayoutState.mOffset
            fill(recycler, state)
            endPosition = mLayoutState.mPosition
            if (mLayoutState.mCalibratedOffset != Int.MIN_VALUE) {
                endOffset = mLayoutState.mCalibratedOffset
            }
            startOffset = mLayoutState.mOffset
        } else {
            //System.out.println("layoutFromStart");
            updateLayoutStateToFillEnd(
                mAnchorInfo.mPosition,
                mAnchorInfo.mBaseCoordinate,
                mAnchorInfo.mShouldAddCenterOffset
            )
            startPosition = mLayoutState.mPosition
            startOffset = mLayoutState.mOffset
            fill(recycler, state)
            endPosition = mLayoutState.mPosition
            if (mLayoutState.mCalibratedOffset != Int.MIN_VALUE) {
                startOffset = mLayoutState.mCalibratedOffset
            }
            endOffset = mLayoutState.mOffset
        }
        val hasLayoutAll = startPosition == 0 && endPosition == state.itemCount
        if (!hasLayoutAll && !mLayoutState.mForceToLayoutInfinitely) {
            //在已有布局好的View的情况下，从View中获取AnchorInfo时，可能出现的因为边界导致的布局中断，需要再次layout进行补充
            //如果第一次布局完成时发现所有的item已经被布局出来了，就不再重复布局
            while (endOffset < width) {
                //System.out.println("patchUpForEnd -> endOffset : " + endOffset);
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END
                updateLayoutStateInfinity(state)
                fill(recycler, state)
                endOffset = mLayoutState.mOffset
            }
            while (startOffset > 0) {
                //System.out.println("patchUpForStart -> startOffset : " + startOffset);
                mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START
                updateLayoutStateInfinity(state)
                fill(recycler, state)
                startOffset = mLayoutState.mOffset
            }
        } else {
            if (endOffset < width) {
                mLayoutState.mForceToLayoutInfinitely = true
            }
            while (endOffset < width) {
                updateLayoutStateToFillEndInfinity(state)
                fill(recycler, state)
                endOffset = mLayoutState.mOffset
            }
            if (startOffset > 0) {
                mLayoutState.mForceToLayoutInfinitely = true
            }
            while (startOffset > 0) {
                updateLayoutStateToFillStartInfinity(state)
                fill(recycler, state)
                startOffset = mLayoutState.mOffset
            }
        }

        //System.out.println("startOffset : " + startOffset + " ;  endOffset : " + endOffset);
        layoutForPredictiveAnimations(recycler, state, startOffset, endOffset)
        calculateBaseCenterX(childCountBeforeLayout, childCount)
        checkIfNotEnoughToScrollInfinitely(childCountBeforeLayout, childCount, state.itemCount)
        //System.out.println("onLayoutChildren[END] -> childCount :" + getChildCount());
        //System.out.println("--------------------------------------------------------");
    }

    private fun calculateBaseCenterX(childCountBeforeLayout: Int, childCountAfterLayout: Int) {
        if (childCountBeforeLayout == 0 && childCountAfterLayout > 0) {
            val view = getChildAt(0)
            if (view != null) {
                if (basePosition == BASE_POSITION_CENTER) {
                    centerX = viewHelper.center
                } else if (basePosition == BASE_POSITION_START) {
                    centerX = viewHelper.getDecoratedCenterHorizontal(view)
                }
            }
        }
    }

    private fun checkIfNotEnoughToScrollInfinitely(
        childCountBeforeLayout: Int,
        childCountAfterLayout: Int,
        itemCount: Int
    ) {
        if (childCountBeforeLayout == 0 && childCountAfterLayout > 0) {
            mTransformRangeCalculated = false
            if (itemCount <= 1) {
                mIsItemInsufficient = true
            }
            if (itemCount < childCountAfterLayout) {
                mIsNeedToFixScrollingDirection = true
                mIsItemNotEnough = true
            }
        } else {
            if (mTransformRangeCalculated) {
                notifyScrollOffsetChanged()
            } else {
                calculateTransformRange()
            }
        }
    }

    private fun calculateTransformRange() {
        if (childCount == 0 || mTransformRangeCalculated) {
            return
        }
        var centerView: View? = null
        var centerX = viewHelper.center
        var leftX = Int.MIN_VALUE
        var rightX = Int.MIN_VALUE
        if (basePosition == BASE_POSITION_START) {
            val first = getChildAt(0)
            if (first != null) {
                centerX = viewHelper.getDecoratedCenterHorizontal(first)
                val right = getChildAt(1)
                if (right != null) {
                    rightX = viewHelper.getDecoratedCenterHorizontal(right)
                    leftX = centerX - (rightX - centerX)
                }
                centerView = first
            }
        } else {
            val center = findReferenceChildClosestToCenter()
            if (center != null) {
                val centerIndex = getChildIndex(center)
                val left = getChildAt(centerIndex - 1)
                val right = getChildAt(centerIndex + 1)
                if (left != null && right != null) {
                    leftX = viewHelper.getDecoratedCenterHorizontal(left)
                    rightX = viewHelper.getDecoratedCenterHorizontal(right)
                } else {
                    leftX = -centerX
                    rightX = if (width > 0) {
                        width + centerX
                    } else {
                        centerX * 3
                    }
                }
                centerView = center
            }
        }
        if (centerView != null && leftX != Int.MIN_VALUE && rightX != Int.MIN_VALUE && mViewTransformListener != null) {
            mViewTransformListener.onFirstLayout(centerX, leftX, rightX)
            notifyScrollOffsetChanged()
            mTransformRangeCalculated = true
        }
    }

    private fun calibrateViewIndex() {
        if (childCount == 0) return
        for (i in 0 until childCount) {
            val view = getChildAt(i)
            view?.let { v ->
                val childCenter = (v.right - v.left) / 2f + v.left
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //不支持API<21的情况修改ZIndex
                    v.z = calculateViewZIndexByPosition(childCenter)
                    println("View[${getChildIndex(v)}] : ${v.z}")
                }
            }
        }
    }

    private fun calculateViewZIndexByPosition(childCenter: Float): Float {
        return 2f / (abs(childCenter - centerX) + 1f)
    }

    private fun updateLayoutStateToFillStartInfinity(state: RecyclerView.State) {
        val firstChild = getChildAt(0)
        val lastChild = getChildAt(childCount - 1)
        if (firstChild == null || lastChild == null) return
        val left = viewHelper.getDecoratedStart(firstChild)
        val firstChildPosition = getPosition(firstChild)

        mLayoutState.mAvailable = left
        if (firstChildPosition == 0) {
            mLayoutState.mPosition = state.itemCount - 1
        } else {
            mLayoutState.mPosition = firstChildPosition - 1
        }
        mLayoutState.mOffset = left
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mShouldAddCenterOffset = false
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
    }

    private fun updateLayoutStateToFillEndInfinity(state: RecyclerView.State) {
        val firstChild = getChildAt(0)
        val lastChild = getChildAt(childCount - 1)
        if (firstChild == null || lastChild == null) return
        val right = viewHelper.getDecoratedEnd(lastChild)
        val firstChildPosition = getPosition(firstChild)
        val lastChildPosition = getPosition(lastChild)
        //System.out.println("updateLayoutStateToFillEndInfinity");
        //System.out.println("firstChild[" + getPosition(firstChild) + "], lastChild[" + getPosition(lastChild) + "]");
        mLayoutState.mAvailable = width - right
        if (lastChildPosition == state.itemCount - 1) {
            mLayoutState.mPosition = 0
        } else {
            mLayoutState.mPosition = firstChildPosition
        }
        mLayoutState.mOffset = right
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mShouldAddCenterOffset = false
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
        //System.out.println("mPosition : " + mLayoutState.mPosition + " ; mOffset : " + mLayoutState.mOffset + " ; mAvailable : " + mLayoutState.mAvailable);
    }

    private fun updateLayoutStateToFillStart(
        itemPosition: Int,
        offset: Int,
        shouldAddCenterOffset: Boolean
    ) {
        mLayoutState.mAvailable = offset
        mLayoutState.mPosition = itemPosition
        mLayoutState.mOffset = offset
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_START
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mShouldAddCenterOffset = shouldAddCenterOffset
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
        mLayoutState.mIsHeadItem = true
    }

    private fun updateLayoutStateToFillEnd(
        itemPosition: Int,
        offset: Int,
        shouldAddCenterOffset: Boolean
    ) {
        mLayoutState.mAvailable = width - offset
        mLayoutState.mPosition = itemPosition
        mLayoutState.mOffset = offset
        mLayoutState.mLayoutDirection = LayoutState.LAYOUT_END
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mShouldAddCenterOffset = shouldAddCenterOffset
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
        mLayoutState.mIsHeadItem = true
    }

    private fun updateAnchorInfoForLayout(state: RecyclerView.State) {
        if (updateAnchorFromPendingData(state)) {
            return
        }
        if (updateAnchorFromChildren(state)) {
            return
        }
        //默认起点为中心
        mAnchorInfo.mBaseCoordinate = width / 2
        mAnchorInfo.mShouldAddCenterOffset = true
        if (basePosition == BASE_POSITION_START) {
            mAnchorInfo.mBaseCoordinate = 0
            mAnchorInfo.mShouldAddCenterOffset = false
        }
        mAnchorInfo.mPosition = 0
    }

    /**
     * 用于在调用[RecyclerView.scrollToPosition]时正确的布局
     *
     * @param state [RecyclerView.State]
     * @return 当有上述方法被触发时会返回true, 默认为false
     */
    private fun updateAnchorFromPendingData(state: RecyclerView.State): Boolean {
        if (state.isPreLayout || mPendingScrollPosition == RecyclerView.NO_POSITION) {
            return false
        }
        mAnchorInfo.mPosition = mPendingScrollPosition
        if (mPendingScrollOffset == Int.MIN_VALUE) {
            val child = findViewByPosition(mPendingScrollPosition)
            return if (child != null) {
                if (basePosition == BASE_POSITION_CENTER) {
                    mAnchorInfo.mBaseCoordinate = width / 2
                    mAnchorInfo.mShouldAddCenterOffset = true
                    return true
                }
                val startGap = viewHelper.getDecoratedStart(child)
                if (startGap < 0) {
                    mAnchorInfo.mBaseCoordinate = 0
                    return true
                }
                val endGap = viewHelper.getDecoratedEnd(child) - width
                if (endGap > 0) {
                    mAnchorInfo.mBaseCoordinate = width
                    mAnchorInfo.mLayoutFromEnd = true
                    return true
                }
                false
            } else {
                val positionToEnd = abs(mAnchorInfo.mPosition - (state.itemCount - 1))
                if (basePosition == BASE_POSITION_CENTER) {
                    mAnchorInfo.mBaseCoordinate = width / 2
                    mAnchorInfo.mShouldAddCenterOffset = true
                } else if (positionToEnd < mAnchorInfo.mPosition || mAnchorInfo.mPosition == state.itemCount - 1) {
                    mAnchorInfo.mLayoutFromEnd = true
                    mAnchorInfo.mBaseCoordinate = width
                } else {
                    mAnchorInfo.mBaseCoordinate = 0
                }
                true
            }
        }
        mAnchorInfo.mBaseCoordinate = mPendingScrollOffset
        return true
    }

    private fun updateAnchorFromChildren(state: RecyclerView.State): Boolean {
        if (childCount == 0) {
            return false
        }
        val view =
            if (mLayoutState.mForceToLayoutInfinitely) findReferenceChildClosestToCenter() else findReferenceChild(
                state
            )
        if (view != null) {
            mAnchorInfo.mShouldAddCenterOffset = false
            mAnchorInfo.assign(viewHelper.getDecoratedStart(view), getPosition(view))
            if (!state.isPreLayout && supportsPredictiveItemAnimations()) {
                val notVisible =
                    viewHelper.getDecoratedStart(view) >= width || viewHelper.getDecoratedEnd(view) < 0
                if (notVisible) {
                    mAnchorInfo.mBaseCoordinate = 0
                }
            }
            return true
        }
        return false
    }

    private fun getChildIndex(view: View): Int {
        var index = 0
        val adapterPosition = getPosition(view)
        for (i in 0 until childCount) {
            val v = getChildAt(0)
            if (v != null) {
                val position = getPosition(v)
                if (position == adapterPosition) {
                    index = i
                    break
                }
            }
        }
        return index
    }

    private fun findReferenceChild(state: RecyclerView.State): View {
        var invalidMatch: View? = null
        var outOfBoundsMatch: View? = null
        val boundsStart = 0
        val boundsEnd = width
        for (i in 0 until childCount) {
            val view = getChildAt(i) ?: continue
            val position = getPosition(view)
            if (position >= 0 && position < state.itemCount) {
                if ((view.layoutParams as RecyclerView.LayoutParams).isItemRemoved) {
                    if (invalidMatch == null) {
                        invalidMatch = view //已经移除的View，非优先选择
                    }
                } else if (viewHelper.getDecoratedStart(view) >= boundsEnd || viewHelper.getDecoratedEnd(
                        view
                    ) < boundsStart
                ) {
                    if (outOfBoundsMatch == null) {
                        outOfBoundsMatch = view //移动到不可见区域的View，非优先选择
                    }
                } else {
                    return view
                }
            }
        }
        return outOfBoundsMatch ?: invalidMatch!!
    }

    private fun findReferenceChildClosestToCenter(): View? {
        val childCount = childCount
        val centerX = centerX
        var closestChild: View? = null
        var absClosest = Int.MAX_VALUE
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenter = viewHelper.getDecoratedCenterHorizontal(child)
            val absDistance = abs(childCenter - centerX)
            if (absDistance < absClosest) {
                absClosest = absDistance
                closestChild = child
                //val closedPosition = getPosition(closestChild)
                //System.out.println("closed child position : " + closedPosition);
            }
        }
        return closestChild
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return scrollBy(dx, recycler, state)
    }

    private fun scrollBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        //System.out.println("scrollBy -> dx: " + dx);
        if (childCount == 0 || dx == 0) {
            return 0
        }
        //dx, fromPosition, toPosition
        mLayoutState.mShouldRecycle = true
        mLayoutState.mIsScrollBy = true
        mLayoutState.mLayoutDirection =
            if (dx > 0) LayoutState.LAYOUT_END else LayoutState.LAYOUT_START
        val absDx = abs(dx)
        updateLayoutState(absDx)
        val scrollingOffsetX = mLayoutState.mScrollingOffsetX
        var consumed = scrollingOffsetX + fill(recycler, state)
        if (consumed <= 0 || mLayoutState.mPosition == state.itemCount || mLayoutState.mPosition == -1) {
            updateLayoutStateInfinityWhenScroll(absDx, state)
            fill(recycler, state)
            consumed = absDx
        }
        val scrolled = if (absDx > consumed) consumed * mLayoutState.mLayoutDirection else dx
        offsetChildrenHorizontal(-scrolled)
        notifyScrollOffsetChanged()
        mLayoutState.mLastScrollDelta = dx
        mLayoutState.mIsScrollBy = false
        return scrolled
    }

    private fun notifyScrollOffsetChanged() {
        if (childCount <= 0 || mViewTransformListener == null) return
        for (i in 0 until childCount) {
            val v = getChildAt(i) ?: continue
            val centerX = viewHelper.getDecoratedCenterHorizontal(v)
            val (first, second) = mViewTransformListener.getScale(centerX.toFloat(), 0f)
            v.scaleX = first
            v.scaleY = second
            val alpha = mViewTransformListener.getAlpha(centerX.toFloat(), 0f)
            v.alpha = alpha
        }
        calibrateViewIndex()
    }

    private fun updateLayoutState(absDx: Int) {
        val startChild = getChildAt(0)
        val endChild = getChildAt(childCount - 1)
        if (startChild == null || endChild == null) {
            return
        }
        val scrollingOffset: Int
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mPosition = getPosition(startChild) + mLayoutState.mLayoutDirection
            mLayoutState.mOffset = viewHelper.getDecoratedStart(startChild)
            scrollingOffset = -viewHelper.getDecoratedStart(startChild)
        } else {
            mLayoutState.mPosition = getPosition(endChild) + mLayoutState.mLayoutDirection
            mLayoutState.mOffset = viewHelper.getDecoratedEnd(endChild)
            scrollingOffset = viewHelper.getDecoratedEnd(endChild) - width
        }
        mLayoutState.mAvailable = absDx - scrollingOffset
        mLayoutState.mScrollingOffsetX = scrollingOffset
    }

    private fun updateLayoutStateInfinity(state: RecyclerView.State) {
        val firstChild = getChildAt(0)
        val lastChild = getChildAt(childCount - 1)
        if (firstChild == null || lastChild == null) return
        val firstChildPosition = getPosition(firstChild)
        val lastChildPosition = getPosition(lastChild)
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mAvailable = viewHelper.getDecoratedStart(firstChild)
            mLayoutState.mOffset = viewHelper.getDecoratedStart(firstChild)
            mLayoutState.mPosition = firstChildPosition - 1
            if (firstChildPosition == 0) {
                mLayoutState.mPosition = state.itemCount - 1
            }
        } else {
            mLayoutState.mAvailable = width - viewHelper.getDecoratedEnd(lastChild)
            mLayoutState.mOffset = viewHelper.getDecoratedEnd(lastChild)
            mLayoutState.mPosition = lastChildPosition + 1
            if (lastChildPosition == state.itemCount - 1) {
                mLayoutState.mPosition = 0
            }
        }
        mLayoutState.mShouldAddCenterOffset = false
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
    }

    private fun updateLayoutStateInfinityWhenScroll(available: Int, state: RecyclerView.State) {
        val startChild = getChildAt(0)
        val endChild = getChildAt(childCount - 1)
        if (startChild == null || endChild == null) {
            return
        }
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            mLayoutState.mPosition = state.itemCount - 1
            mLayoutState.mOffset = viewHelper.getDecoratedStart(startChild)
        } else {
            mLayoutState.mPosition = 0
            mLayoutState.mOffset = viewHelper.getDecoratedEnd(endChild)
        }
        mLayoutState.mShouldAddCenterOffset = false
        mLayoutState.mAvailable = available
        mLayoutState.mScrollingOffsetX = Int.MIN_VALUE
        mLayoutState.mCalibratedOffset = Int.MIN_VALUE
    }

    private fun fill(recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        //System.out.println("-----------------------------------------");
        //System.out.println("fill -> width: " + getWidth() + " ; childCount : " + getChildCount() + " ; adapterItemCount : " + state.getItemCount());
        //System.out.println("scrollOffsetX : " + mLayoutState.mScrollingOffsetX);
        if (mLayoutState.mScrollingOffsetX != Int.MIN_VALUE) {
            if (mLayoutState.mAvailable < 0) {
                mLayoutState.mScrollingOffsetX += mLayoutState.mAvailable
                //这里的最终计算结果等于dx
            }
            recycleByLayoutState(recycler)
        }
        val start = mLayoutState.mAvailable
        var remainingSpace = start + mLayoutState.mExtraFillSpace
        while (remainingSpace > 0 && mLayoutState.hasMore(state)) {
            mLayoutChunkResult.reset()
            layoutChunk(recycler)
            //System.out.println("layoutChunk -> childCount : " + getChildCount());
            if (mLayoutState.mShouldAddCenterOffset) {
                mLayoutState.mOffset -= mLayoutChunkResult.mOffset * mLayoutState.mLayoutDirection
                mLayoutState.mCalibratedOffset = mLayoutState.mOffset
                mLayoutState.mAvailable += mLayoutChunkResult.mOffset
                remainingSpace += mLayoutChunkResult.mOffset
                mLayoutState.mShouldAddCenterOffset = false
            }
            val totalConsumed = mLayoutChunkResult.mConsumed + mLayoutChunkResult.mExtraConsumed
            mLayoutState.mOffset += totalConsumed * mLayoutState.mLayoutDirection
            //mLayoutState.mOffset += mLayoutChunkResult.mScaleConsumed * mLayoutState.mLayoutDirection;
            mLayoutState.mAvailable -= totalConsumed
            remainingSpace -= totalConsumed
            if (mLayoutState.mScrollingOffsetX != Int.MIN_VALUE) {
                mLayoutState.mScrollingOffsetX += totalConsumed
                if (mLayoutState.mAvailable < 0) {
                    mLayoutState.mScrollingOffsetX += mLayoutState.mAvailable
                }
                recycleByLayoutState(recycler)
            }
        }
        //System.out.println("after fill -> childCount : " + getChildCount());
        //System.out.println("--------------------------------------------");
        return start - mLayoutState.mAvailable
    }

    private fun layoutChunk(recycler: RecyclerView.Recycler) {
        //System.out.println("layoutChunk");
        val view = mLayoutState.nextView(recycler)
        if (mLayoutState.mScrapList == null) {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                addView(view, 0)
            } else {
                addView(view)
            }
        } else {
            if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
                addDisappearingView(view, 0)
            } else {
                addDisappearingView(view)
            }
        }
        measureChildWithMargins(view!!, 0, 0)
        val measuredWidth = viewHelper.getDecoratedMeasuredWidth(view)
        val measuredHeight = viewHelper.getDecoratedMeasuredHeight(view)
        var left: Int
        var right: Int
        val top = (height - measuredHeight) / 2
        val bottom = top + measuredHeight
        mLayoutChunkResult.mConsumed = measuredWidth
        /*int scaleOffset = 0;
        if (!mLayoutState.mFirstLayout) {
            scaleOffset = getScale(view, measuredWidth);
        }*/
        //System.out.println("view width : " + measuredWidth + " ; height : " + measuredHeight);
        var offsetForLayoutCenter = 0
        if (basePosition == BASE_POSITION_CENTER) {
            offsetForLayoutCenter = measuredWidth / 2
        }
        mLayoutChunkResult.mOffset = offsetForLayoutCenter
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            right = mLayoutState.mOffset
            if (mLayoutState.mShouldAddCenterOffset) {
                right += offsetForLayoutCenter
                mLayoutState.mIsHeadItem = false
            } else {
                if (mLayoutState.mIsHeadItem) {
                    mLayoutState.mIsHeadItem = false
                } else {
                    right -= mItemSpace
                    mLayoutChunkResult.mExtraConsumed = mItemSpace
                }
            }
            left = right - measuredWidth
        } else {
            left = mLayoutState.mOffset
            if (mLayoutState.mShouldAddCenterOffset) {
                left -= offsetForLayoutCenter
                mLayoutState.mIsHeadItem = false
            } else {
                if (mLayoutState.mIsHeadItem) {
                    mLayoutState.mIsHeadItem = false
                } else {
                    left += mItemSpace
                    mLayoutChunkResult.mExtraConsumed = mItemSpace
                }
            }
            right = left + measuredWidth
        }
        //mLayoutChunkResult.mScaleConsumed = scaleOffset / 2;
        //onScaleChanged(view, measuredWidth, mLayoutState.mFirstLayout);
        //mLayoutState.mScaleOffset = onScaleChanged(view, measuredWidth) / 2;
        //mLayoutChunkResult.mExtraConsumed += onScaleChanged(view, measuredWidth, mLayoutState.mFirstLayout) / 2;
        layoutDecoratedWithMargins(view, left, top, right, bottom)
    }

    private fun recycleByLayoutState(recycler: RecyclerView.Recycler) {
        if (!mLayoutState.mShouldRecycle) {
            return
        }
        val threshold: Int
        if (mLayoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
            threshold = width - mLayoutState.mScrollingOffsetX
            for (i in childCount - 1 downTo 0) {
                val child = getChildAt(i) ?: continue
                if (viewHelper.getDecoratedStart(child) < threshold) {
                    recycleChildren(recycler, childCount - 1, i)
                    return
                }
            }
        } else {
            threshold = mLayoutState.mScrollingOffsetX
            for (i in 0 until childCount - 1) {
                val child = getChildAt(i) ?: continue
                if (viewHelper.getDecoratedEnd(child) > threshold) {
                    recycleChildren(recycler, 0, i)
                    return
                }
            }
        }
    }

    private fun recycleChildren(recycler: RecyclerView.Recycler, start: Int, end: Int) {
        if (start == end) return
        if (start < end) {
            for (i in end - 1 downTo start) {
                removeAndRecycleViewAt(i, recycler)
            }
        } else {
            for (i in start downTo end + 1) {
                removeAndRecycleViewAt(i, recycler)
            }
        }
    }

    override fun findViewByPosition(position: Int): View? {
        val childCount = childCount
        if (childCount == 0) {
            return null
        }
        var rightBorder = childCount - 1
        var index = 0
        var tailIndex = childCount - 1
        var firstView = getChildAt(index) ?: return null
        var lastView = getChildAt(tailIndex) ?: return null
        val selectNextOne = isForceToScrollToRight || mIsItemInsufficient
        var firstChildPosition = getPosition(firstView)
        var lastChildPosition = getPosition(lastView)
        if (selectNextOne) {
            while (viewHelper.isOutOfBounds(firstView)) {
                index++
                firstView = getChildAt(index) ?: continue
                firstChildPosition = getPosition(firstView)
            }
            while (viewHelper.isOutOfBounds(lastView)) {
                tailIndex--
                rightBorder--
                lastView = getChildAt(tailIndex) ?: continue
                lastChildPosition = getPosition(lastView)
            }
        }
        val basePosition = if (selectNextOne) lastChildPosition else firstChildPosition
        val viewPosition = position - basePosition
        if (viewPosition in (0 until childCount)) {
            val childIndex = if (selectNextOne) rightBorder - viewPosition else viewPosition
            val targetView = getChildAt(childIndex)
            if (targetView == null || getPosition(targetView) > basePosition) {
                return null
            } else if (getPosition(targetView) == position) {
                return targetView
            }
        }
        return super.findViewByPosition(position)
    }

    override fun scrollToPosition(position: Int) {
        mPendingScrollPosition = position
        mPendingScrollOffset = Int.MIN_VALUE
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        if (state.itemCount == 0) {
            return
        }
        val smoothScroller = GallerySmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        if (childCount == 0) {
            return null
        }
        val firstChild = getChildAt(0)
        val lastChild = getChildAt(childCount - 1)
        if (firstChild == null || lastChild == null) {
            return null
        }
        val firstChildPosition = getPosition(firstChild)
        var direction = -1
        val lastChildPosition = getPosition(lastChild)
        if (firstChildPosition > lastChildPosition && targetPosition >= firstChildPosition) {
            direction = 1
        }
        if (mIsNeedToFixScrollingDirection || isForceToScrollToRight) {
            direction = 1
        }
        return PointF(direction.toFloat(), 0f)
    }

    override fun onAdapterChanged(
        oldAdapter: RecyclerView.Adapter<*>?,
        newAdapter: RecyclerView.Adapter<*>?
    ) {
        super.onAdapterChanged(oldAdapter, newAdapter)
        mIsItemInsufficient = false
        mIsItemNotEnough = false
        mIsNeedToFixScrollingDirection = false
        mLayoutState.mShouldCheckTransformParams = true
    }

    private fun layoutForPredictiveAnimations(
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State,
        startOffset: Int,
        endOffset: Int
    ) {
        if (!state.willRunPredictiveAnimations() || state.isPreLayout || childCount == 0 || !supportsPredictiveItemAnimations()) {
            return
        }
        //System.out.println("layoutForPredictiveAnimations -> startOffset : " + startOffset + " ; endOffset : " + endOffset);
        //为了正确的执行动画，需要将由于数据产生变化而被暂且剥离并移入Recycler.scrapList中的View布局出来
        //这种情况通常由添加Item将原本的Item挤出可见区域，或者已有的Item扩展将其他的Item挤出可见区域导致
        var scrapExtraForStart = 0
        var scrapExtraForEnd = 0
        val scrapList = recycler.scrapList
        val firstChild = getChildAt(0) ?: return
        val firstChildPos = getPosition(firstChild)
        for (i in scrapList.indices) {
            val scrap = scrapList[i]
            if ((scrap.itemView.layoutParams as RecyclerView.LayoutParams).isItemRemoved) {
                continue
            }
            val position = scrap.layoutPosition
            val direction =
                if (position < firstChildPos) LayoutState.LAYOUT_START else LayoutState.LAYOUT_END
            if (direction == LayoutState.LAYOUT_START) {
                scrapExtraForStart += viewHelper.getDecoratedMeasuredWidth(scrap.itemView)
            } else {
                scrapExtraForEnd += viewHelper.getDecoratedMeasuredWidth(scrap.itemView)
            }
        }
        mLayoutState.mScrapList = scrapList
        if (scrapExtraForStart > 0) {
            val anchor = getChildAt(0) ?: return
            updateLayoutStateToFillStart(getPosition(anchor), startOffset, false)
            mLayoutState.mExtraFillSpace = scrapExtraForStart
            mLayoutState.mAvailable = 0
            mLayoutState.assignViewFromScrapList()
            fill(recycler, state)
        }
        if (scrapExtraForEnd > 0) {
            val anchor = getChildAt(childCount - 1) ?: return
            updateLayoutStateToFillEnd(getPosition(anchor), endOffset, false)
            mLayoutState.mExtraFillSpace = scrapExtraForEnd
            mLayoutState.mAvailable = 0
            mLayoutState.assignViewFromScrapList()
            fill(recycler, state)
        }
        mLayoutState.mScrapList = null
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)
        mPendingScrollPosition = RecyclerView.NO_POSITION
        mPendingScrollOffset = Int.MIN_VALUE
        mAnchorInfo.reset()
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        mSnapHelper?.attachToRecyclerView(view)
    }

    override fun onScrollStateChanged(state: Int) {
        when (state) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                if (mSnapHelper != null) {
                    val view = mSnapHelper.findSnapView(this)
                    if (view != null) {
                        val adapterPosition = getPosition(view)
                        if (mOnScrollListener != null) {
                            mOnScrollListener?.onIdle(adapterPosition)
                        }
                    }
                }
            }
            RecyclerView.SCROLL_STATE_DRAGGING -> {
                mOnScrollListener?.onDragging()
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                mOnScrollListener?.onSettling()
            }
        }
    }

    class Builder {
        var itemSpace = 0

        /**
         * 布局基准点，默认为可视区域中心，即选中的Item在中心位置
         */
        var basePosition = BASE_POSITION_CENTER
        var forceToScrollToRight = true
        var snapHelper: SnapHelper? = GallerySnapHelper()
        var onScrollListener: OnScrollListener? = null
        var viewTransformListener: ViewTransformListener? = null

        fun setItemSpace(itemSpace: Int): Builder {
            this.itemSpace = itemSpace
            return this
        }

        fun setForceToScrollToRight(forceToScrollToRight: Boolean): Builder {
            this.forceToScrollToRight = forceToScrollToRight
            return this
        }

        fun setBasePosition(@BasePosition basePosition: Int): Builder {
            this.basePosition = basePosition
            return this
        }

        fun setSnapHelper(snapHelper: SnapHelper?): Builder {
            this.snapHelper = snapHelper
            return this
        }

        fun setOnScrollListener(onScrollListener: OnScrollListener?): Builder {
            this.onScrollListener = onScrollListener
            return this
        }

        fun setViewTransformListener(viewTransformListener: ViewTransformListener?): Builder {
            this.viewTransformListener = viewTransformListener
            return this
        }

        fun build(): GalleryLayoutManager {
            val layoutManager = GalleryLayoutManager(
                snapHelper,
                forceToScrollToRight,
                itemSpace,
                basePosition,
                viewTransformListener
            )
            layoutManager.onScrollListener = onScrollListener
            return layoutManager
        }
    }

    private class GallerySmoothScroller(context: Context?) :
        LinearSmoothScroller(context) {
        override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
            val layoutManager = layoutManager
            if (layoutManager == null || !layoutManager.canScrollHorizontally() || layoutManager !is GalleryLayoutManager) {
                return 0
            }
            //System.out.println("calculateDxToMakeVisible -> snapPreference : " + snapPreference);
            val left = layoutManager.viewHelper.getDecoratedStart(view)
            val right = layoutManager.viewHelper.getDecoratedEnd(view)
            val start = layoutManager.paddingLeft
            val end = layoutManager.width - layoutManager.paddingRight
            val center = layoutManager.viewHelper.center
            val shouldOffsetToCenter = layoutManager.basePosition == BASE_POSITION_CENTER
            return calculateDtToFit(
                left,
                right,
                center,
                start,
                end,
                snapPreference,
                shouldOffsetToCenter
            )
        }

        private fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxCenter: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int,
            shouldOffsetToCenter: Boolean
        ): Int {
            val viewWidthHalf = (viewEnd - viewStart) / 2
            when (snapPreference) {
                SNAP_TO_START -> {
                    //System.out.println("SNAP_TO_START : ");
                    return if (shouldOffsetToCenter) {
                        boxCenter - viewStart + viewWidthHalf
                    } else boxStart - viewStart
                }
                SNAP_TO_END -> {
                    //System.out.println("SNAP_TO_END : ");
                    return if (shouldOffsetToCenter) {
                        boxCenter - viewEnd + viewWidthHalf
                    } else boxEnd - viewEnd
                }
                SNAP_TO_ANY -> {
                    //System.out.println("SNAP_TO_ANY -> shouldOffsetToCenter : " + shouldOffsetToCenter);
                    if (shouldOffsetToCenter) {
                        val dtStartC = boxCenter - viewWidthHalf - viewStart
                        if (dtStartC > 0) {
                            return dtStartC
                        }
                        val dtEndC = boxCenter + viewWidthHalf - viewEnd
                        if (dtEndC < 0) {
                            return dtEndC
                        }
                    }
                    val dtStart = boxStart - viewStart
                    if (dtStart > 0) {
                        return dtStart
                    }
                    val dtEnd = boxEnd - viewEnd
                    if (dtEnd < 0) {
                        return dtEnd
                    }
                }
                else -> throw IllegalArgumentException(
                    "snap preference should be one of the"
                            + " constants defined in SmoothScroller, starting with SNAP_"
                )
            }
            return 0
        }
    }

    private class GallerySnapHelper : SnapHelper() {
        private var mLayout: GalleryLayoutManager? = null

        fun attachToLayoutManager(layoutManager: GalleryLayoutManager?) {
            mLayout = layoutManager
        }

        override fun calculateDistanceToFinalSnap(
            layoutManager: RecyclerView.LayoutManager,
            view: View
        ): IntArray {
            return mLayout?.let { layout ->
                val distance = IntArray(2)
                val childCenter = layout.viewHelper.getDecoratedCenterHorizontal(view)
                //System.out.println("calculateDistanceToFinalSnap -> centerX : " + mLayout.getCenterX());
                if (layout.isItemNotEnough()) {
                    val vc = layout.findReferenceChildClosestToCenter()
                    vc?.let { vcc ->
                        //println("index : " + layout.getChildIndex(vcc) + " ; left : " + vcc.left)
                        distance[0] = (vcc.right - vcc.left) / 2 + vcc.left - layout.centerX
                    }
                } else {
                    distance[0] = childCenter - layout.centerX
                }
                //distance[0] = childCenter - layout.centerX
                //System.out.println("calculateDistanceToFinalSnap -> distanceX : " + distance[0]);
                return distance
            } ?: IntArray(0)
        }

        override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
            return mLayout?.let { layout ->
                if (layout.childCount == 0) {
                    null
                } else {
                    val childCount = layout.childCount
                    val centerX = layout.centerX
                    //System.out.println("findSnapView -> centerX : " + centerX);
                    var closestChild: View? = null
                    var absClosest = Int.MAX_VALUE
                    for (i in 0 until childCount) {
                        val child = layout.getChildAt(i) ?: continue
                        val childCenter =
                            if (layout.isItemNotEnough()) ((child.right - child.left) / 2 + child.left) else layout.viewHelper.getDecoratedCenterHorizontal(
                                child
                            )
                        val absDistance = abs(childCenter - centerX)
                        if (absDistance < absClosest) {
                            absClosest = absDistance
                            closestChild = child
                        }
                    }
                    closestChild
                }
            } ?: let {
                null
            }
        }

        override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager,
            velocityX: Int,
            velocityY: Int
        ): Int {
            return mLayout?.let { layout ->
                if (layout.childCount == 0) {
                    RecyclerView.NO_POSITION
                } else {
                    val itemCount = layout.childCount

                    // A child that is exactly in the center is eligible for both before and after
                    var closestChildBeforeCenter: View? = null
                    var distanceBefore = Int.MIN_VALUE
                    var closestChildAfterCenter: View? = null
                    var distanceAfter = Int.MAX_VALUE

                    // Find the first view before the center, and the first view after the center
                    val childCount = layoutManager.childCount
                    for (i in 0 until childCount) {
                        val child = layoutManager.getChildAt(i) ?: continue
                        val distance = distanceToCenter(layout, child)
                        if (distance <= 0 && distance > distanceBefore) {
                            // Child is before the center and closer then the previous best
                            distanceBefore = distance
                            closestChildBeforeCenter = child
                        }
                        if (distance >= 0 && distance < distanceAfter) {
                            // Child is after the center and closer then the previous best
                            distanceAfter = distance
                            closestChildAfterCenter = child
                        }
                    }

                    // Return the position of the first child from the center, in the direction of the fling
                    val forwardDirection = isForwardFling(layoutManager, velocityX, velocityY)
                    if (forwardDirection && closestChildAfterCenter != null) {
                        return layoutManager.getPosition(closestChildAfterCenter)
                    } else if (!forwardDirection && closestChildBeforeCenter != null) {
                        return layoutManager.getPosition(closestChildBeforeCenter)
                    }

                    // There is no child in the direction of the fling. Either it doesn't exist (start/end of
                    // the list), or it is not yet attached (very rare case when children are larger then the
                    // viewport). Extrapolate from the child that is visible to get the position of the view to
                    // snap to.
                    val visibleView =
                        (if (forwardDirection) closestChildBeforeCenter else closestChildAfterCenter)
                            ?: return RecyclerView.NO_POSITION
                    val visiblePosition = layoutManager.getPosition(visibleView)
                    val snapToPosition = visiblePosition + if (forwardDirection) 1 else -1
                    if (snapToPosition < 0 || snapToPosition >= itemCount) {
                        RecyclerView.NO_POSITION
                    } else snapToPosition
                }
            } ?: RecyclerView.NO_POSITION
        }

        private fun distanceToCenter(layoutManager: GalleryLayoutManager, targetView: View): Int {
            val childCenter = layoutManager.viewHelper.getDecoratedCenterHorizontal(targetView)
            val containerCenter = layoutManager.centerX
            return childCenter - containerCenter
        }

        private fun isForwardFling(
            layoutManager: RecyclerView.LayoutManager,
            velocityX: Int,
            velocityY: Int
        ): Boolean {
            return if (layoutManager.canScrollHorizontally()) {
                velocityX > 0
            } else {
                velocityY > 0
            }
        }
    }

    class ViewHelper internal constructor(var mLayoutManager: RecyclerView.LayoutManager) {
        fun getDecoratedMeasuredWidth(view: View): Int {
            val lp = view.layoutParams as RecyclerView.LayoutParams
            return mLayoutManager.getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin
        }

        fun getDecoratedMeasuredHeight(view: View): Int {
            val lp = view.layoutParams as RecyclerView.LayoutParams
            return mLayoutManager.getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin
        }

        fun getDecoratedStart(view: View): Int {
            val lp = view.layoutParams as RecyclerView.LayoutParams
            return mLayoutManager.getDecoratedLeft(view) - lp.leftMargin
        }

        fun getDecoratedEnd(view: View): Int {
            val lp = view.layoutParams as RecyclerView.LayoutParams
            return mLayoutManager.getDecoratedRight(view) + lp.rightMargin
        }

        fun getDecoratedCenterHorizontal(view: View): Int {
            return getDecoratedStart(view) + getDecoratedMeasuredWidth(view) / 2
        }

        val center: Int
            get() = mLayoutManager.width / 2

        fun isOutOfBounds(view: View): Boolean {
            val start = getDecoratedStart(view)
            val end = getDecoratedEnd(view)
            return if (start < 0 && end < 0) {
                true
            } else start > mLayoutManager.width && end > mLayoutManager.width
        }
    }

    private class AnchorInfo {
        var mBaseCoordinate = Int.MIN_VALUE
        var mPosition = -1
        var mExtraSpace = 0
        var mLayoutFromEnd = false
        var mShouldAddCenterOffset = false
        fun reset() {
            mBaseCoordinate = Int.MIN_VALUE
            mPosition = -1
            mExtraSpace = 0
            mLayoutFromEnd = false
            mShouldAddCenterOffset = false
        }

        fun assign(coordinate: Int, position: Int) {
            mBaseCoordinate = coordinate
            mPosition = position
        }
    }

    private class LayoutChunkResult {
        var mConsumed = 0
        var mOffset = 0
        var mExtraConsumed = 0
        var mScaleConsumed = 0
        fun reset() {
            mConsumed = 0
            mOffset = 0
            mExtraConsumed = 0
            mScaleConsumed = 0
        }
    }

    private class LayoutState {
        var mOffset = 0
        var mCalibratedOffset = Int.MIN_VALUE
        var mPosition = 0 //默认序列的起始位置，用于从适配器中获取view
        var mScrollingOffsetX = Int.MIN_VALUE //滑动偏移值
        var mAvailable = 0 //剩余布局空间
        var mExtraFillSpace = 0 //额外的填充空间
        var mLayoutDirection = LAYOUT_END //布局方向
        var mLastScrollDelta = 0
        var mIsScrollBy = false
        var mFirstLayout = false
        var mIsHeadItem = false
        var mShouldAddCenterOffset = false
        var mShouldRecycle = true //是否需要在填充View前后进行回收操作
        var mShouldCheckTransformParams = true
        var mForceToLayoutInfinitely = false //在item数量不足以铺满RecyclerView可视区域时，强制填充重复Item以达到无限循环的效果
        var mScrapList: List<RecyclerView.ViewHolder>? = null //仅用于动画的布局阶段使用的

        fun hasMore(state: RecyclerView.State): Boolean {
            return mPosition >= 0 && mPosition < state.itemCount
        }

        fun nextView(recycler: RecyclerView.Recycler): View? {
            if (mScrapList != null) {
                return nextViewFromScrapList()
            }
            val view = recycler.getViewForPosition(mPosition)
            mPosition += mLayoutDirection
            return view
        }

        fun nextViewFromScrapList(): View? {
            mScrapList?.let { list ->
                for (i in list.indices) {
                    val view = list[i].itemView
                    val lp = view.layoutParams as RecyclerView.LayoutParams
                    if (lp.isItemRemoved) {
                        continue
                    }
                    if (mPosition == lp.viewLayoutPosition) {
                        assignViewFromScrapList(view)
                        return view
                    }
                }
            }
            return null
        }

        fun assignViewFromScrapList(ignore: View? = null) {
            val closest = nextViewInLimitedList(ignore)
            mPosition = if (closest == null) {
                RecyclerView.NO_POSITION
            } else {
                (closest.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
            }
        }

        fun nextViewInLimitedList(ignore: View?): View? {
            return mScrapList?.let { list ->
                val size = list.size
                var closest: View? = null
                var closestDistance = Int.MAX_VALUE
                for (i in 0 until size) {
                    val view = list[i].itemView
                    val lp = view.layoutParams as RecyclerView.LayoutParams
                    if (view === ignore || lp.isItemRemoved) {
                        continue
                    }
                    val distance = lp.viewLayoutPosition - mPosition
                    if (distance < 0) {
                        continue
                    }
                    if (distance < closestDistance) {
                        closest = view
                        closestDistance = distance
                        if (distance == 0) {
                            break
                        }
                    }
                }
                closest
            }
        }

        companion object {
            const val LAYOUT_START = -1
            const val LAYOUT_END = 1
        }
    }
}