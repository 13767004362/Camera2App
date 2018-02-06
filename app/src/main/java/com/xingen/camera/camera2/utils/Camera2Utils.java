package com.xingen.camera.camera2.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.os.Build;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;

import com.xingen.camera.camera2.compare.CompareSizeByArea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ${xingen} on 2017/10/20.
 *
 * Camera2 API中一些计算
 */

public class Camera2Utils {


    /**
     * 检查是否支持设备自动对焦
     * <p>
     * 很多设备的前摄像头都有固定对焦距离，而没有自动对焦。
     *
     * @param characteristics
     * @return
     */
    public static boolean checkAutoFocus(CameraCharacteristics characteristics) {
        int[] afAvailableModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (afAvailableModes.length == 0 || (afAvailableModes.length == 1 && afAvailableModes[0] == CameraMetadata.CONTROL_AF_MODE_OFF)) {
            return  false;
        } else {
             return  true;
        }
    }

    /**
     * 检查相机支持哪几种focusMode
     * @param cameraCharacteristics
     */
    public  void checkFocusMode(CameraCharacteristics cameraCharacteristics){
        int[] availableFocusModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        for (int focusMode : availableFocusModes != null ? availableFocusModes : new int[0]) {
            if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_OFF) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_MACRO) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {

            }
            else if (focusMode == CameraCharacteristics.CONTROL_AF_MODE_AUTO) {

            }
        }
    }
    /**
     * 计算合适的大小Size,在相机拍照
     *
     * @param choices
     * @param textureViewWidth
     * @param textureViewHeight
     * @param maxWidth
     * @param maxHeight
     * @param aspectRatio
     * @return
     */
    public static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight,
                                         int maxWidth, int maxHeight, Size aspectRatio,CompareSizeByArea compareSizesByArea) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, compareSizesByArea);
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough,compareSizesByArea);
        } else {
            Log.e(" 计算结果", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation 屏幕的方向
     * @return JPEG的方向(例如：0,90,270,360)
     */
    public static int getOrientation(SparseIntArray ORIENTATIONS, int mSensorOrientation,int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

   public  static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    /**
     * 计算合适的大小，在视频录制
     * @param choices
     * @param width
     * @param height
     * @param aspectRatio
     * @return
     */
    public  static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    /**
     * 匹配指定方向的摄像头，前还是后
     *
     * LENS_FACING_FRONT是前摄像头标志
     * @param cameraCharacteristics
     * @param direction
     * @return
     */
    public static  boolean matchCameraDirection(CameraCharacteristics cameraCharacteristics,int direction){
        //这里设置后摄像头
        Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
         return  (facing != null && facing == direction)?true:false;
    }

    /**
     * 获取相机支持最大的调焦距离
     * @param cameraCharacteristics
     * @return
     */
    public  static  Float getMinimumFocusDistance(CameraCharacteristics cameraCharacteristics){
        Float distance=null;
        try {
         distance= cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return  distance;
    }

    /**
     * 获取最大的数字变焦值，也就是缩放值
     * @param cameraCharacteristics
     * @return
     */
    public static  Float getMaxZoom(CameraCharacteristics cameraCharacteristics){
        Float maxZoom=null;
        try {
          maxZoom= cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        }catch (Exception e){
            e.printStackTrace();
        }
        return maxZoom;
    }

    /**
     * 用于检查是否支持Camera 2
     *
     * 事实上，在各个厂商的的Android设备上，Camera2的各种特性并不都是可用的，
     * 需要通过characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)方法来根据返回值来获取支持的级别：
     *
     * 1. INFO_SUPPORTED_HARDWARE_LEVEL_FULL：全方位的硬件支持，允许手动控制全高清的摄像、支持连拍模式以及其他新特性。
     * 2. INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED：有限支持，这个需要单独查询。
     * 3. INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY：所有设备都会支持，也就是和过时的Camera API支持的特性是一致的。
     *
     *
     * @param mContext
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasCamera2(Context mContext) {
        if (mContext == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        try {
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            String[] idList = manager.getCameraIdList();
            boolean notFull = true;
            if (idList.length == 0) {
                notFull = false;
            } else {
                for (final String str : idList) {
                    if (str == null || str.trim().isEmpty()) {
                        notFull = false;
                        break;
                    }
                    final CameraCharacteristics characteristics = manager.getCameraCharacteristics(str);
                    final int supportLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notFull = false;
                        break;
                    }
                }
            }
            return notFull;
        } catch (Throwable ignore) {
            return false;
        }
    }


    /**
     * 计算zoom所对应的rect
     * @param originReact 相机原始的rect
     * @param currentZoom 当前的zoom值
     * @return
     */
     public static Rect createZoomRect(Rect originReact,float currentZoom){
        Rect zoomRect=null;
        try {
            if (originReact==null){
                return zoomRect;
            }else{
                float ratio=(float)1/currentZoom;
                int cropWidth=originReact.width()-Math.round((float)originReact.width() * ratio);
                int cropHeight=originReact.height() - Math.round((float)originReact.height() * ratio);
                zoomRect = new Rect(cropWidth/2, cropHeight/2, originReact.width() - cropWidth/2, originReact.height() - cropHeight/2);
            }
        }catch (Exception e){
            e.printStackTrace();
            zoomRect=null;
        }
        return zoomRect;
     }


}
