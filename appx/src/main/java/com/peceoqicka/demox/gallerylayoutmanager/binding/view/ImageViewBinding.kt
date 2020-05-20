package com.peceoqicka.demox.gallerylayoutmanager.binding.view

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.peceoqicka.demox.gallerylayoutmanager.util.TintUtil
import java.io.File

@BindingAdapter("imageResource")
fun ImageView.bindImageResource(res: Int) {
    Glide.with(this.context).load(res).into(this)
}

@BindingAdapter("imageResource", "drawableTint")
fun ImageView.bindImageResourceWithTint(res: Int, colorStateList: ColorStateList) {
    val drawable = ContextCompat.getDrawable(this.context, res)
    if (drawable != null) {
        this.setImageDrawable(TintUtil.tintDrawable(drawable, colorStateList))
    }
}

@BindingAdapter("imageDrawable")
fun ImageView.bindImageDrawable(drawable: Drawable?) {
    Glide.with(this.context).load(drawable).into(this)
}

@BindingAdapter("imageDrawable", "drawableTint")
fun ImageView.bindImageDrawableWithTint(drawable: Drawable?, colorStateList: ColorStateList?) {
    if (drawable != null && colorStateList != null) {
        this.setImageDrawable(TintUtil.tintDrawable(drawable, colorStateList))
    }
}

@BindingAdapter("imageDrawable", "shouldFillSrc")
fun ImageView.setImageDrawable(drawable: Drawable?, shouldFillSrc: Boolean) {
    if (shouldFillSrc) {
        Glide.with(this.context).load(drawable).into(this)
    }
}

@BindingAdapter("imageUrl")
fun ImageView.setImageUrl(imageUrl: String?) {
    Glide.with(this.context).load(imageUrl).into(this)
}

@BindingAdapter("imageUrl", "imageDefault", "imageShape")
fun setImageURLWithShape(imageView: ImageView, url: String, defaultDrawable: Drawable, shape: Int) {
    Glide.with(imageView.context).load(if (url.isNotEmpty()) url else defaultDrawable)
        .apply(
            when (shape) {
                0 -> RequestOptions.circleCropTransform()
                else -> RequestOptions.circleCropTransform()
            }
        )
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