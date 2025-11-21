package com.xingen.camera.widget

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView

/**
 * Created by ${xinGen} on 2017/10/19.
 *
 * 自定义一个可以调整到指定长宽比的TextureView.
 */
class AutoFitTextureView : TextureView {
    private var mRatioWidth = 0
    private var mRatioHeight = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * 根据指定的长宽比例来设置TextureView的长宽
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth)
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height)
            }
        }
    }

    /**
     * 设置长宽比率，1:2与2：4是一样的效果。
     *
     * @param width
     * @param height
     */
    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        mRatioWidth = width
        mRatioHeight = height
        requestLayout()
    }
}
