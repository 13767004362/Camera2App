package com.xingen.camera.camera2.compare;

import android.util.Size;

import java.util.Comparator;

/**
 * Created by ${xingen} on 2017/10/21.
 *
 * 比较器：比较区域面积大小
 */

public class CompareSizeByArea implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        //确保乘法不会溢出范围
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                (long) rhs.getWidth() * rhs.getHeight());
    }
}