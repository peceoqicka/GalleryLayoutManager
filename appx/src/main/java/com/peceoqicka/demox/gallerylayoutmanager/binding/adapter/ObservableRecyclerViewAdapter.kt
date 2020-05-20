package com.peceoqicka.demox.gallerylayoutmanager.binding.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

/**
 * <pre>
 *      author  :   peceoqicka
 *      time    :   2018/8/14
 *      version :   1.0
 *      desc    :   RecyclerView使用的通用数据适配器（DataBinding）
 *                  数据更新时，自动通知RecyclerView刷新
 * </pre>
 */
abstract class ObservableRecyclerViewAdapter<Data, in Binding>(var dataList: ObservableArrayList<Data>) : RecyclerView.Adapter<ObservableRecyclerViewAdapter.BindingViewHolder>() where Binding : ViewDataBinding {
    var simpleOnItemClick: ((Data) -> Unit)? = null
    var animOnItemClick: ((View, Data) -> Unit)? = null

    private lateinit var listChangedCallback: ListChangedCallback<Data, Binding>
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

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        createListChangedListener()
        this.dataList.addOnListChangedCallback(listChangedCallback)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.dataList.removeOnListChangedCallback(listChangedCallback)
    }

    private fun createListChangedListener() {
        if (!this::listChangedCallback.isInitialized) {
            listChangedCallback = ListChangedCallback(this)
        }
    }

    override fun getItemCount(): Int = dataList.size

    open fun onItemClick(itemView: View, data: Data) {
        if (simpleOnItemClick != null) {
            simpleOnItemClick?.invoke(data)
        } else if (animOnItemClick != null) {
            animOnItemClick?.invoke(itemView, data)
        }
    }

    open fun resetData(data: ObservableArrayList<Data>) {
        this.dataList.clear()
        this.dataList.addAll(data)
    }

    open fun moreItem(data: ObservableArrayList<Data>) {
        dataList.addAll(data)
    }

    open fun removeItem(data: Data) {
        dataList.remove(data)
    }

    open fun removeAllItems() {
        dataList.clear()
    }

    class ListChangedCallback<Data, Binding>(val adapter: ObservableRecyclerViewAdapter<Data, Binding>) : ObservableList.OnListChangedCallback<ObservableArrayList<Data>>() where Binding : ViewDataBinding {
        override fun onChanged(sender: ObservableArrayList<Data>) =
                adapter.notifyDataSetChanged()

        override fun onItemRangeRemoved(sender: ObservableArrayList<Data>, positionStart: Int, itemCount: Int) =
                adapter.notifyItemRangeRemoved(positionStart, itemCount)

        override fun onItemRangeMoved(sender: ObservableArrayList<Data>, fromPosition: Int, toPosition: Int, itemCount: Int) =
                if (itemCount == 1) adapter.notifyItemMoved(fromPosition, toPosition)
                else adapter.notifyDataSetChanged()

        override fun onItemRangeInserted(sender: ObservableArrayList<Data>, positionStart: Int, itemCount: Int) =
                adapter.notifyItemRangeInserted(positionStart, itemCount)

        override fun onItemRangeChanged(sender: ObservableArrayList<Data>, positionStart: Int, itemCount: Int) =
                adapter.notifyItemRangeChanged(positionStart, itemCount)
    }

    class BindingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}