package com.peceoqicka.gallerylayoutmanager

import android.view.View

/**
 * View变形的工具
 */
interface ViewTransformListener {
    /**
     * 初始状态获取变形公式所需参数
     * [centerX]  itemView处于此位置时将获得缩放scaleX和scaleY，以及selectedAlpha
     * [leftX]    itemView处于此位置到centerX范围内时，由缩放值1f逐渐变化到scaleX(scaleY)
     * [rightX]   与leftX同理
     * itemView处于小于leftX的位置或大于rightX时，缩放值均为1f
     */
    fun onFirstLayout(centerX: Int, leftX: Int, rightX: Int)

    /**
     * 获取对应位置的缩放比例
     */
    fun getScale(x: Float, y: Float): Pair<Float, Float>

    /**
     * 获取对应位置的透明度
     */
    fun getAlpha(x: Float, y: Float): Float
}