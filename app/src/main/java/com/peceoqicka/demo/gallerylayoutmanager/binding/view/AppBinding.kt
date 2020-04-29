package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.databinding.BindingConversion
import android.graphics.drawable.ColorDrawable

/**
 * Created by b.b.d on 2018/8/29.
 */
@BindingConversion
fun convertColorToDrawable(color: Int): ColorDrawable {
    return ColorDrawable(color)
}

//@BindingAdapter("app:niv_indicatorCount")
//fun NumberIndicatorView.bindIndicatorCount(count: Int) {
//    this.setIndicatorCount(count)
//}
//
//@BindingAdapter("app:niv_selectedIndicator")
//fun NumberIndicatorView.bindSelectedIndicator(selection: Int) {
//    this.selection = selection
//}