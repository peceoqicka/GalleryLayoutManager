package com.peceoqicka.demo.gallerylayoutmanager.util

import android.app.Activity
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.billy.android.swipe.SmartSwipe
import com.billy.android.swipe.consumer.ActivitySlidingBackConsumer
import com.peceoqicka.demo.gallerylayoutmanager.R
import com.peceoqicka.demo.gallerylayoutmanager.data.NewsModel
import org.jetbrains.anko.dimen

fun Activity.color(colorResId: Int): Int {
    return ContextCompat.getColor(this, colorResId)
}

fun Activity.toast(text: CharSequence) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Activity.toast(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
}

fun Activity.toastLong(text: CharSequence) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

fun Activity.toastLong(resId: Int) {
    Toast.makeText(this, resId, Toast.LENGTH_LONG).show()
}

fun Activity.addSwipeSlidingBack(){
    SmartSwipe.wrap(this)
        .addConsumer(ActivitySlidingBackConsumer(this))
        .setRelativeMoveFactor(0.5f)
        .setEdgeSize(dimen(R.dimen.px_200))
        .enableLeft()
}

fun <T> NewsModel.toList(transform: (NewsModel.Item) -> T): ArrayList<T> {
    val list = arrayListOf<T>()
    this.data.mapTo(list, transform)
    return list
}