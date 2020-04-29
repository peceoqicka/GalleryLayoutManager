package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.databinding.BindingAdapter
import android.graphics.Color
import android.view.View
import android.view.ViewGroup

/**
 * <pre>
 *      author  :   peceoqicka
 *      time    :   2018/8/17
 *      version :   1.0
 *      desc    :
 * </pre>
 */
@BindingAdapter("android:layout_width")
fun setLayoutWidth(view: View, layoutWidth: Float) {
    view.layoutParams.width = layoutWidth.toInt()
}

@BindingAdapter("android:layout_height")
fun setLayoutHeight(view: View, layoutHeight: Float) {
    view.layoutParams.height = layoutHeight.toInt()
}


@BindingAdapter("android:layout_marginTop")
fun setLayoutMarginTop(view: View, layoutMarginTop: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.topMargin = layoutMarginTop.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_marginStart")
fun setLayoutMarginStart(view: View, layoutMarginStart: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.marginStart = layoutMarginStart.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_marginEnd")
fun setLayoutMarginEnd(view: View, layoutMarginEnd: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.marginEnd = layoutMarginEnd.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_marginBottom")
fun setLayoutMarginBottom(view: View, layoutMarginBottom: Float) {
    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
    layoutParams.bottomMargin = layoutMarginBottom.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("app:selected")
fun setSelected(view: View, isSelected: Boolean) {
    view.isSelected = isSelected
}

@BindingAdapter("app:colorStr")
fun View.bindColorString(colorString: String) {
    this.setBackgroundColor(Color.parseColor(colorString))
}

@BindingAdapter("app:colorInt")
fun View.bindColorInt(colorInt: Int) {
    this.setBackgroundColor(colorInt)
}