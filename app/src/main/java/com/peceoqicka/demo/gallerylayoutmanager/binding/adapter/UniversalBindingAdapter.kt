package com.peceoqicka.demo.gallerylayoutmanager.binding.adapter

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * <pre>
 *      author  :   E.M
 *      e-mail  :   ratchet@qq.com
 *      time    :   2018/4/19
 *      desc    :   通用适配器(DataBinding版),不支持多 View Type
 *      version :   1.0
 * </pre>
 */
abstract class UniversalBindingAdapter<Data, in Binding>(var dataList: ArrayList<Data>) : RecyclerView.Adapter<UniversalBindingAdapter.BindingViewHolder>() where Binding : ViewDataBinding {
    var simpleOnItemClick: ((Data) -> Unit)? = null
    var animOnItemClick: ((View, Data) -> Unit)? = null

    protected abstract fun getLayoutId(): Int
    protected abstract fun onSetData(binding: Binding, data: Data)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val binding = DataBindingUtil.inflate<Binding>(LayoutInflater.from(parent.context),
                getLayoutId(), parent, false)
        return BindingViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        DataBindingUtil.getBinding<Binding>(holder.itemView)?.let { binding ->
            onSetData(binding, dataList[position])
            binding.executePendingBindings()
        }
    }

    fun removeAll() {
        dataList.removeAll(dataList)
    }

    fun addAll(dataList: ArrayList<Data>) {
        this.dataList.addAll(dataList)
    }

    fun refresh(dataList: ArrayList<Data>) {
        removeAll()
        addAll(dataList)
    }

    override fun getItemCount(): Int = dataList.size

    open fun onItemClick(itemView: View, data: Data) {
        if (simpleOnItemClick != null) {
            simpleOnItemClick?.invoke(data)
        } else if (animOnItemClick != null) {
            animOnItemClick?.invoke(itemView, data)
        }
    }

    class BindingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}