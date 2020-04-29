package com.peceoqicka.demo.gallerylayoutmanager.activity.main

import android.databinding.BaseObservable
import android.databinding.Bindable
import com.android.databinding.library.baseAdapters.BR

class SquareItemViewModel : BaseObservable() {
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