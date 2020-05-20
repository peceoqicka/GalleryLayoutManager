package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.databinding.BindingConversion
import android.graphics.drawable.ColorDrawable

@BindingConversion
fun convertColorToDrawable(color: Int): ColorDrawable {
    return ColorDrawable(color)
}