package com.peceoqicka.demo.gallerylayoutmanager.activity.first

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.android.databinding.library.baseAdapters.BR

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