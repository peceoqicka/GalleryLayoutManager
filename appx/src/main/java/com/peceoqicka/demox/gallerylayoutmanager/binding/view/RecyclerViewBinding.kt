package com.peceoqicka.demox.gallerylayoutmanager.binding.view

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper

@BindingAdapter("adapter")
fun <VH : RecyclerView.ViewHolder> RecyclerView.bindAdapter(adapter: RecyclerView.Adapter<VH>?) {
    this.adapter = adapter
}

@BindingAdapter("layoutManager")
fun RecyclerView.bindLayoutManager(layoutManager: RecyclerView.LayoutManager) {
    this.layoutManager = layoutManager
}

@BindingAdapter("itemDecoration")
fun RecyclerView.bindItemDecoration(itemDecoration: RecyclerView.ItemDecoration) {
    this.addItemDecoration(itemDecoration)
}

@BindingAdapter("itemAnimator")
fun RecyclerView.bindItemAnimator(itemAnimator: RecyclerView.ItemAnimator) {
    this.itemAnimator = itemAnimator
}

@BindingAdapter("snapHelper")
fun RecyclerView.bindSnapHelper(snapHelper: SnapHelper) {
    snapHelper.attachToRecyclerView(this)
}