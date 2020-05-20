package com.peceoqicka.demo.gallerylayoutmanager.binding.view

import android.content.res.ColorStateList
import android.databinding.BindingAdapter
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.peceoqicka.demo.gallerylayoutmanager.util.TintUtil
import java.io.File

@BindingAdapter("imageRes")
fun setImageRes(imageView: ImageView, res: Int) {
    if (res == 0) {
        imageView.visibility = View.GONE
    } else {
        Glide.with(imageView.context).load(res).into(imageView)
    }
}

@BindingAdapter("imageRes", "drawableTint")
fun setImageResWithTint(imageView: ImageView, res: Int, colorStateList: ColorStateList?) {
    val drawable = ContextCompat.getDrawable(imageView.context, res)
    if (colorStateList != null && drawable != null) {
        imageView.setImageDrawable(TintUtil.tintDrawable(drawable, colorStateList))
    }
}

@BindingAdapter("imageDrawable")
fun setImageDrawable(imageView: ImageView, drawable: Drawable?) {
    drawable?.let {
        imageView.setImageDrawable(drawable)
    }
}

@BindingAdapter("imageDrawable", "drawableTint")
fun setImageDrawableWithTint(imageView: ImageView, drawable: Drawable?, colorStateList: ColorStateList?) {
    if (drawable != null && colorStateList != null) {
        imageView.setImageDrawable(TintUtil.tintDrawable(drawable, colorStateList))
    }
}

@BindingAdapter("imageDrawable", "shouldFillSrc")
fun setImageDrawable(imageView: ImageView, drawable: Drawable?, shouldFillSrc: Boolean) {
    if (shouldFillSrc) {
        imageView.setImageDrawable(drawable)
    }
}

@BindingAdapter("imageUrl")
fun setImageUrl(imageView: ImageView, imageUrl: String?) {
    if ("" == imageUrl)
        imageView.visibility = View.GONE
    else {
        Glide.with(imageView.context).load(imageUrl).into(imageView)
    }
}

@BindingAdapter("imageUrl", "imageDefault", "imageShape")
fun setImageURLWithShape(imageView: ImageView, url: String, defaultDrawable: Drawable, shape: Int) {
    Glide.with(imageView.context).load(if (url.isNotEmpty()) url else defaultDrawable)
            .apply(when (shape) {
                0 -> RequestOptions.circleCropTransform()
                else -> RequestOptions.circleCropTransform()
            })
            .into(imageView)
}

@BindingAdapter("imageLocation")
fun setImageLocation(imageView: ImageView, imageLocation: String) {
    if (TextUtils.isEmpty(imageLocation)) {
        imageView.visibility = View.GONE
    } else {
        Glide.with(imageView.context).load(File(imageLocation)).into(imageView)
    }
}