package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.databinding.BindingAdapter
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SnapHelper

@BindingAdapter("adapter")
fun <VH : RecyclerView.ViewHolder> setRecyclerViewAdapter(
    recyclerView: RecyclerView,
    adapter: RecyclerView.Adapter<VH>?
) {
    if (adapter != null) {
        recyclerView.adapter = adapter
    }
}

@BindingAdapter("layoutManager")
fun setLayoutManager(recyclerView: RecyclerView, layoutManager: RecyclerView.LayoutManager) {
    recyclerView.layoutManager = layoutManager
}

@BindingAdapter("itemDecoration")
fun addItemDecoration(recyclerView: RecyclerView, itemDecoration: RecyclerView.ItemDecoration?) {
    if (itemDecoration != null) {
        recyclerView.addItemDecoration(itemDecoration)
    }
}

@BindingAdapter("itemAnimator")
fun setItemAnimator(recyclerView: RecyclerView, itemAnimator: RecyclerView.ItemAnimator) {
    recyclerView.itemAnimator = itemAnimator
}

@BindingAdapter("snapHelper")
fun RecyclerView.bindSnapHelper(snapHelper: SnapHelper) {
    snapHelper.attachToRecyclerView(this)
}