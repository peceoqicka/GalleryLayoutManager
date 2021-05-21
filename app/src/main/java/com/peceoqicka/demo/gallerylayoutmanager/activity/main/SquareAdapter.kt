package com.peceoqicka.demo.gallerylayoutmanager.activity.main

import com.peceoqicka.demo.gallerylayoutmanager.R
import com.peceoqicka.demo.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demo.gallerylayoutmanager.databinding.ItemSquareBinding

class SquareAdapter(data: MutableList<SquareItemViewModel>) :
    UniversalBindingAdapter<SquareItemViewModel, ItemSquareBinding>(data) {
    override fun getLayoutId(): Int {
        return R.layout.item_square
    }

    override fun onSetData(binding: ItemSquareBinding, data: SquareItemViewModel) {
        binding.model = data
    }
}