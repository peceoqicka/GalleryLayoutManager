package com.peceoqicka.demox.gallerylayoutmanager.activity.first

import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ItemFirstScaleBinding

class FirstScaleAdapter(data: ArrayList<ItemFirstScaleViewModel>) :
    UniversalBindingAdapter<ItemFirstScaleViewModel, ItemFirstScaleBinding>(data) {
    override fun getLayoutId(): Int {
        return R.layout.item_first_scale
    }

    override fun onSetData(binding: ItemFirstScaleBinding, data: ItemFirstScaleViewModel) {
        binding.model = data
    }
}