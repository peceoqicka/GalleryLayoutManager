package com.peceoqicka.demox.gallerylayoutmanager.binding.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

/**
 * <pre>
 *      author  :   peceoqicka
 *      time    :   2018/5/2
 *      version :   1.0
 *      desc    :   除[RecyclerView]以外的一般[AdapterView]使用的万能适配器
 * </pre>
 */
abstract class UniversalAdapter<Data, in Binding>(var dataList: ArrayList<Data>) : BaseAdapter() where Binding : ViewDataBinding {
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
}