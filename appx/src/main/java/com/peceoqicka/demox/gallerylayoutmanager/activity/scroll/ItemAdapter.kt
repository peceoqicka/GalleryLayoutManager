package com.peceoqicka.demox.gallerylayoutmanager.activity.scroll

import com.peceoqicka.demox.gallerylayoutmanager.R
import com.peceoqicka.demox.gallerylayoutmanager.binding.adapter.UniversalBindingAdapter
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ItemAutoScrollBinding
import com.peceoqicka.demox.gallerylayoutmanager.databinding.ItemSquareBinding

class ItemAdapter(data: ArrayList<ItemViewModel>) :
    UniversalBindingAdapter<ItemViewModel, ItemAutoScrollBinding>(data) {
    override fun getLayoutId(): Int {
        return R.layout.item_auto_scroll
    }

    override fun onSetData(binding: ItemAutoScrollBinding, data: ItemViewModel) {
        binding.model = data
    }
}