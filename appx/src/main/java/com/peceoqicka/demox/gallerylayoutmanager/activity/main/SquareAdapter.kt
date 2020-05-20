package com.peceoqicka.demox.gallerylayoutmanager.activity.main

import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ItemSquareBinding

class SquareAdapter(data: ArrayList<SquareItemViewModel>) :
    UniversalBindingAdapter<SquareItemViewModel, ItemSquareBinding>(data) {
    override fun getLayoutId(): Int {
        return R.layout.item_square
    }

    override fun onSetData(binding: ItemSquareBinding, data: SquareItemViewModel) {
        binding.model = data
    }
}