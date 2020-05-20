package com.peceoqicka.demox.gallerylayoutmanager.data

import com.peceoqicka.demox.gallerylayoutmanager.activity.main.SquareItemViewModel

data class NewsModel(var data: List<Item>) {
    data class Item(var imageUrl: String, var title: String)

    fun toItemViewModel(): ArrayList<SquareItemViewModel> {
        val list = arrayListOf<SquareItemViewModel>()
        data.mapTo(list) { item ->
            SquareItemViewModel().apply {
                this.imageUrl = item.imageUrl
                this.title = item.title
            }
        }
        return list
    }
}