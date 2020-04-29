package com.peceoqicka.demo.gallerylayoutmanager.binding.adapter

import android.databinding.DataBindingUtil
import android.databinding.ObservableArrayList
import android.databinding.ViewDataBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

/**
 * <pre>
 *      author  :   peceoqicka
 *      time    :   2018/8/21
 *      version :   1.0
 *      desc    :   标准AdapterView使用的通用数据适配器（DataBinding）
 *                  数据更新时，自动通知AdapterView刷新
 *                  不支持多ViewType
 * </pre>
 */
abstract class ObservableBaseAdapter<Data, in Binding>(var dataList: ObservableArrayList<Data>) : BaseAdapter() where Binding : ViewDataBinding {
    protected abstract fun getLayoutId(): Int
    protected abstract fun onSetData(binding: Binding, data: Data)

    override fun getCount() = dataList.size

    override fun getItem(position: Int): Data = dataList[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: Binding? =
                if (convertView == null)
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                            getLayoutId(), parent, false)
                else
                    DataBindingUtil.getBinding(convertView)

        binding?.also { b ->
            onSetData(b, getItem(position))
            b.executePendingBindings()
        }
        return binding?.root ?: View(parent.context)
    }

    open fun onItemClick(itemView: View, data: Data) {}

    fun resetData(data: ObservableArrayList<Data>) {
        this.dataList.clear()
        this.dataList.addAll(data)
    }

    fun moreItem(data: ObservableArrayList<Data>) {
        dataList.addAll(data)
    }

    fun removeItem(data: Data) {
        dataList.remove(data)
    }

    fun removeAllItems() {
        dataList.clear()
    }
}