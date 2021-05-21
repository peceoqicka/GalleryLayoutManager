package com.peceoqicka.gallerylayoutmanager

/**
 * 简易实现的View变形规则
 *
 * [scaleX] 选中项缩放的大小
 * [scaleY] 选中项缩放的大小
 * [unselectedAlpha] 未选中项的透明度
 * [selectedAlpha] 选中项的透明度
 */
class SimpleViewTransformListener(
    private val scaleX: Float = 1f, private val scaleY: Float = 1f,
    private val unselectedAlpha: Float = 0.5f, private val selectedAlpha: Float = 1f
) : ViewTransformListener {
    private var mCenter: Float = 0f
    private var mLeft: Float = 0f
    private var mRight: Float = 0f

    /**
     * 初始状态获取变形公式所需参数
     * centerX  itemView处于此位置时将获得缩放scaleX和scaleY，以及selectedAlpha
     * leftX    itemView处于此位置到centerX范围内时，由缩放值1f逐渐变化到scaleX(scaleY)
     * rightX   与leftX同理
     * itemView处于小于leftX的位置或大于rightX时，缩放值均为1f
     */
    override fun onFirstLayout(centerX: Int, leftX: Int, rightX: Int) {
        mCenter = centerX * 1f
        mLeft = leftX * 1f
        mRight = rightX * 1f
        //println("onFirstLayout ======> centerX : $centerX ; leftX : $leftX ; rightX : $rightX")
    }

    override fun getScale(x: Float, y: Float): Pair<Float, Float> {
        var scale = Pair(0f, 0f)
        if (!((mCenter > mLeft) && (mCenter < mRight) && (mRight > mLeft))) {
            return scale
        }
        if (scaleX != 1f || scaleY != 1f) {
            val scaleH: Float
            val scaleV: Float
            if (x > mLeft && x < mCenter) {
                val aH = ((scaleX - 1f) / (mCenter - mLeft))
                val aV = ((scaleY - 1f) / (mCenter - mLeft))
                scaleH = aH * x + (1f - aH * mLeft)
                scaleV = aV * x + (1f - aV * mLeft)
            } else if (x == mCenter) {
                scaleH = scaleX
                scaleV = scaleY
            } else if (x > mCenter && x < mRight) {
                val aH = ((scaleX - 1f) / (mCenter - mRight))
                val aV = ((scaleY - 1f) / (mCenter - mRight))
                scaleH = aH * x + (1f - aH * mRight)
                scaleV = aV * x + (1f - aV * mRight)
            } else {
                scaleH = 1f
                scaleV = 1f
            }
            scale = scaleH to scaleV
        }
        return scale
    }

    override fun getAlpha(x: Float, y: Float): Float {
        if (!((mCenter > mLeft) && (mCenter < mRight) && (mRight > mLeft))) {
            return 1f
        }
        var alpha = 1f
        if (unselectedAlpha != selectedAlpha && unselectedAlpha >= 0f && unselectedAlpha <= 1f && selectedAlpha >= 0f && selectedAlpha <= 1f) {
            alpha = if (x > mLeft && x < mCenter) {
                val a = ((selectedAlpha - unselectedAlpha) / (mCenter - mLeft))
                a * x + (unselectedAlpha - a * mLeft)
            } else if (x == mCenter) {
                selectedAlpha
            } else if (x > mCenter && x < mRight) {
                val a = ((selectedAlpha - unselectedAlpha) / (mCenter - mRight))
                a * x + (unselectedAlpha - a * mRight)
            } else {
                unselectedAlpha
            }
        }
        return alpha
    }
}