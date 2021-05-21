package com.peceoqicka.gallerylayoutmanager

interface OnScrollListener {
    /**
     * 无操作闲置时的回调
     *
     * @param snapViewPosition 选中的ItemView在适配器中的位置，仅在设置了SnapHelper的情况下有效
     */
    fun onIdle(snapViewPosition: Int) {}

    /**
     * 由于用户操作或代码调用[androidx.recyclerview.widget.RecyclerView.smoothScrollToPosition]
     * 等产生的滑动过程的回调，通常用于配合自定义的Banner指示器
     * 此回调需要自定义LayoutManager实现具体细节
     *
     * @param scrollingPercentage 滑动距离百分比
     * @param fromPosition        起点位置（适配器位置）
     * @param toPosition          终点位置（适配器位置)
     */
    fun onScrolling(scrollingPercentage: Float, fromPosition: Int, toPosition: Int) {}

    /**
     * 由于用户拖住RecyclerView（没有释放）
     * 具体查看[androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING]
     */
    fun onDragging() {}

    /**
     * RecyclerView正在执行滑动动画，且没有受外力影响
     * 具体查看[androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING]
     */
    fun onSettling() {}
}