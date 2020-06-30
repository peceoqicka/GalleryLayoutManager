package com.peceoqicka.demox.gallerylayoutmanager.activity.scroll

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR

class ItemViewModel : BaseObservable() {
    var bgColor: Int = 0
        @Bindable
        get
        set(value) {
            field = value;notifyPropertyChanged(BR.bgColor)
        }
    var number: Int = 0
        @Bindable
        get
        set(value) {
            field = value;notifyPropertyChanged(BR.number)
        }
    var imageUrl: String = ""
        @Bindable
        get
        set(value) {
            field = value;notifyPropertyChanged(BR.imageUrl)
        }
    var title: String = ""
        @Bindable
        get
        set(value) {
            field = value;notifyPropertyChanged(BR.title)
        }
}