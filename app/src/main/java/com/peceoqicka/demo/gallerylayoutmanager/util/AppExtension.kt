package com.peceoqicka.demo.gallerylayoutmanager.util

import android.app.Activity
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.peceoqicka.demo.gallerylayoutmanager.data.NewsModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

val appMoshi by lazy {
    Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
}

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

fun Activity.readTextFromAssets(name: String): String {
    val inputStream = assets.open(name)
    val inputStreamReader = InputStreamReader(inputStream, Charset.forName("UTF-8"))
    val bufferedReader = BufferedReader(inputStreamReader)
    val text = bufferedReader.readText()
    bufferedReader.close()
    inputStreamReader.close()
    inputStream.close()
    return text
}