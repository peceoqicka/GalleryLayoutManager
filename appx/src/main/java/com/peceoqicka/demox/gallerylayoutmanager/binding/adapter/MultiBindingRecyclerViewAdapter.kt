package com.peceoqicka.demox.gallerylayoutmanager.binding.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * <pre>
 *      author  :   peceoqicka
 *      time    :   2018/4/19
 *      version :   1.0
 *      desc    :   通用适配器(DataBinding版), 支持多 View Type
 * </pre>
 */
abstract class MultiBindingRecyclerViewAdapter :
    RecyclerView.Adapter<MultiBindingRecyclerViewAdapter.MultiViewHolder>() {
    protected abstract fun getLayoutId(viewType: Int): Int
    protected abstract fun onBindView(
        layoutInflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int
    ): View

    protected abstract fun onBindData(holder: MultiViewHolder, position: Int): ViewDataBinding?

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultiViewHolder {
        return MultiViewHolder(onBindView(LayoutInflater.from(parent.context), parent, viewType))
    }

    override fun onBindViewHolder(holder: MultiViewHolder, position: Int) {
        onBindData(holder, position)?.executePendingBindings()
    }

    class MultiViewHolder(view: View) : RecyclerView.ViewHolder(view)
}