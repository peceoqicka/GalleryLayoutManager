package com.peceoqicka.demox.gallerylayoutmanager.activity.first

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.peceoqicka.demox.gallerylayoutmanager.BR

class ItemFirstScaleViewModel : BaseObservable() {
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