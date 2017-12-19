package com.xingen.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * Created by ${xinGen} on 2017/10/19.
 *
 * 自定义一个可以调整到指定长宽比的TextureView.
 */

public class AutoFitTextureView  extends TextureView{
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    public AutoFitTextureView(Context context) {
        super(context);
    }
    public AutoFitTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    /**
     *  根据指定的长宽比例来设置TextureView的长宽
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
    /**
     * 设置长宽比率，1:2与2：4是一样的效果。
     *
     * @param width
     * @param height
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }
}
