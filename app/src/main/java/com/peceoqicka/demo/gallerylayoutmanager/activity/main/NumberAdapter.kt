package com.peceoqicka.demo.gallerylayoutmanager.activity.main

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.peceoqicka.demo.gallerylayoutmanager.BR
import com.peceoqicka.demo.gallerylayoutmanager.R
import com.peceoqicka.demo.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demo.gallerylayoutmanager.databinding.ItemNumberBinding

class NumberAdapter(dataList: MutableList<ItemViewModel>) :
    UniversalBindingAdapter<NumberAdapter.ItemViewModel, ItemNumberBinding>(dataList) {
    override fun getLayoutId(): Int {
        return R.layout.item_number
    }

    override fun onSetData(binding: ItemNumberBinding, data: ItemViewModel) {
        binding.model = data.apply {
            bindAdapter = this@NumberAdapter
        }
    }

    fun addNumber() {
        val currentSize = this.dataList.size
        val addition = ItemViewModel().apply {
            number = currentSize
        }
        addData(addition)
    }

    fun removeNumber() {
        val last = dataList.lastOrNull()
        last?.let { data ->
            removeData(data)
        }
    }

    class ItemViewModel : BaseObservable() {
        lateinit var bindAdapter: NumberAdapter

        @get:Bindable
        var number: Int = 0
            set(value) {
                field = value;notifyPropertyChanged(BR.number)
            }
    }
}