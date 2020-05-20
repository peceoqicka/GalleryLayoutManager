package com.peceoqicka.demox.gallerylayoutmanager.util

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

/**
 * <pre>
 *      author  :   E.M
 *      e-mail  :   ratchet@qq.com
 *      time    :   2018/4/17
 *      desc    :
 *      version :   1.0
 * </pre>
 */
object TintUtil {
    /**
     * 用[colorStateList]对[drawable]进行染色
     *
     * @param drawable      需要渲染的Drawable
     * @param colorStateList 染色用的ColorStateList
     * @return 渲染完成的Drawable
     */
    fun tintDrawable(drawable: Drawable, colorStateList: ColorStateList): Drawable {
        val wrapDrawable = DrawableCompat.wrap(drawable)
        DrawableCompat.setTintList(wrapDrawable, colorStateList)
        return wrapDrawable
    }
}