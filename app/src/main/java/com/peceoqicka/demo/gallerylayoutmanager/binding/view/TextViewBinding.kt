package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.content.res.ColorStateList
import android.databinding.BindingAdapter
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.widget.TextView

/**
 * <pre>
 *      author  :   b.b.d
 *      time    :   2018/9/5
 *      version :   1.0
 *      desc    :   TextView绑定的公共方法
 *                  Modified by peceoqicka 2018/10/10
 * </pre>
 */
@BindingAdapter("app:textResource")
fun setTextResource(textView: TextView, stringResId: Int) {
    textView.setText(stringResId)
}

@BindingAdapter("android:text", "app:textIndex")
fun setTextFromArray(textView: TextView, array: Array<String>?, index: Int) {
    if (index >= 0 && array != null && index < array.size) {
        textView.text = array[index]
    }
}

@BindingAdapter("app:textOrigin", "app:textArray", "app:textIndex")
fun setTextFromArrayOrOrigin(textView: TextView, text: String?, array: Array<String>?, index: Int) {
    if (index >= 0 && array != null && index < array.size) {
        textView.text = array[index]
    } else if (text != null) {
        textView.text = text
    }
}

@BindingAdapter("app:textColor", "app:textColorStateList")
fun setTextColorStateList(textView: TextView, textColor: Int, colorStateList: ColorStateList?) {
    if (colorStateList != null) {
        textView.setTextColor(colorStateList)
    } else {
        textView.setTextColor(textColor)
    }
}

@BindingAdapter("app:tv_html")
fun setHtmlText(textView: TextView, htmlText: String) {
    textView.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY)
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(htmlText)
    }
}

@BindingAdapter("app:text_state")
fun textState(view: TextView, state: String) {
    when (state) {
        "1" -> view.text = "处理中"
        "2" -> view.text = "计划待编"
        "3" -> view.text = "采用"
        "4" -> view.text = "存档"
    }
}

@BindingAdapter("app:color_state")
fun colorState(view: TextView, state: String) {
    when (state) {
        "1" -> view.setTextColor(Color.parseColor("#F98080"))
        "2" -> view.setTextColor(Color.parseColor("#F98080"))
        "3" -> view.setTextColor(Color.parseColor("#4FD249"))
        "4" -> view.setTextColor(Color.parseColor("#4FD249"))
    }
}
