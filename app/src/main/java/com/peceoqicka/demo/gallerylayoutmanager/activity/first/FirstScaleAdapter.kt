package com.peceoqicka.demo.gallerylayoutmanager.activity.first

import com.peceoqicka.demo.gallerylayoutmanager.R
import com.peceoqicka.demo.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demo.gallerylayoutmanager.databinding.ItemFirstScaleBinding

class FirstScaleAdapter(data: ArrayList<ItemFirstScaleViewModel>) :
    UniversalBindingAdapter<ItemFirstScaleViewModel, ItemFirstScaleBinding>(data) {
    override fun getLayoutId(): Int {
        return R.layout.item_first_scale
    }

    override fun onSetData(binding: ItemFirstScaleBinding, data: ItemFirstScaleViewModel) {
        binding.model = data
    }
}