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
 *      time    :   2018/4/19
 *      version :   1.0
 *      desc    :   通用适配器, 支持多 View Type
 * </pre>
 */
abstract class MultiBindingAdapter : BaseAdapter() {
    protected abstract fun getLayoutId(viewType: Int): Int
    protected abstract fun onBindData(binding: ViewDataBinding, position: Int)

    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val binding: ViewDataBinding? =
                if (convertView == null)
                    DataBindingUtil.inflate(LayoutInflater.from(parent.context),
                            getLayoutId(getItemViewType(position)), parent, false)
                else
                    DataBindingUtil.getBinding(convertView)

        if (binding != null) {
            onBindData(binding, position)
            binding.executePendingBindings()
        }
        return binding?.root
    }
}