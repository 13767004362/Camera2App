package com.xingen.camera.camera2;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.view.TextureView;

import com.xingen.camera.camera2.operate.PictureOperater;
import com.xingen.camera.camera2.operate.VideoRecordOperator;
import com.xingen.camera.mode.Constant;
import com.xingen.camera.utils.thread.WorkThreadUtils;
import com.xingen.camera.utils.toast.ToastUtils;

/**
 * Created by ${xingen} on 2017/10/20.
 */

public class Camera2Manager {
    /**
     * 前,后摄像头
     */
    public static final  int CAMERA_DIRECTION_FRONT=4;
    public static  final  int CAMERA_DIRECTION_BACK=5;
    public static final String TAG = Camera2Manager.class.getSimpleName();
    private Context context;
    private WorkThreadUtils workThreadManager;
    private BaseCamera2Operator pictureOperator;
    private BaseCamera2Operator videoRecordOperator;
    private BaseCamera2Operator currentOperator;
    private int currentDirection;
    /**
     * 是否手动调焦
     */
    private boolean isManualFocus;
    /**
     * 焦距比例,手动调焦的模式下
     */
    private float zoomProportion;

    public Camera2Manager(Context context, WorkThreadUtils workThreadManager) {
        this.context = context;
        this.workThreadManager = workThreadManager;
        //默认使用后摄像头
        this.currentDirection=CAMERA_DIRECTION_BACK;
        this.pictureOperator = new PictureOperater(this);
        this.videoRecordOperator = new VideoRecordOperator(this);
        setCurrentCameraDirection(this.currentDirection);
        //默认拍照模式
        this.currentOperator = pictureOperator;
        //是否开启手动调焦
        this.isManualFocus=false;
        this.zoomProportion =0;
    }
    public void onResume(TextureView textureView) {
        this.videoRecordOperator.setWeakReference(textureView);
        this.pictureOperator.setWeakReference(textureView);
        this.currentOperator.startOperate();
    }
    public void onPause() {
        this.currentOperator.stopOperate();
    }
    public void setCamera2ResultCallBack(BaseCamera2Operator.Camera2ResultCallBack camera2ResultCallBack) {
        pictureOperator.setCamera2ResultCallBack(camera2ResultCallBack);
        videoRecordOperator.setCamera2ResultCallBack(camera2ResultCallBack);
    }
    public  void setCameraVideoCallBack(BaseCamera2Operator.Camera2VideoRecordCallBack cameraVideoCallBack){
        ( (VideoRecordOperator) this.videoRecordOperator).setCamera2VideoRecordCallBack(cameraVideoCallBack);
    }

    public void takePictureOrVideo() {
        this.currentOperator.cameraClick();
    }
    /**
     * 给拍照和视频录制两种模式下设置，摄像头方向
     * @param currentDirection
     */
    private void setCurrentCameraDirection(int currentDirection){
        int direction=(currentDirection==CAMERA_DIRECTION_BACK)? CameraCharacteristics.LENS_FACING_BACK:CameraCharacteristics.LENS_FACING_FRONT;
        Log.i(TAG,TAG+" 切换摄像头为："+(direction== CameraCharacteristics.LENS_FACING_BACK?"后":"前"));
        //告诉两个拍照和录像的操作类，记录当前的摄像头。
        this.videoRecordOperator.setCurrentDirection(direction);
        this.pictureOperator.setCurrentDirection(direction);
    }
    /**
     * 暂停视频
     */
    public  void pauseVideoRecord(){
        ((VideoRecordOperator) this.videoRecordOperator).pauseRecordingVideo();
    }

    /**
     * 切换到拍照还是录像模式
     * @param currentMode
     */
    public void switchMode(int currentMode) {
        Log.i(TAG,TAG+" 切换模式是： "+(currentMode==Constant.MODE_CAMERA?"拍照":"录像"));
        switch (currentMode) {
            //切换到拍照模式
            case Constant.MODE_CAMERA:
                this.videoRecordOperator.stopOperate();
                this.currentOperator = this.pictureOperator;
                break;
            //切换到录像模式
            case Constant.MODE_VIDEO_RECORD:
                this.pictureOperator.stopOperate();
                this.currentOperator = this.videoRecordOperator;
                break;
            default:
                break;
        }
        this.currentOperator.startOperate();
    }

    /**
     * 切换摄像头，前还是后
     * @param direction
     */
     public void switchCameraDirection(int direction){
         //相同摄像头方向，不进行操作
         if (currentDirection== direction){
               return;
         }
         //当视频录制状态，不能切换摄像头
         if (currentOperator instanceof  VideoRecordOperator){
             if (((VideoRecordOperator)currentOperator).isVideoRecord()){
                 ToastUtils.showToast(context,"客官，请结束录像，再切换摄像头");
                 return;
             }
         }
         switch (direction){
             case  CAMERA_DIRECTION_BACK:
                 ToastUtils.showToast(context,"客官，请稍等，正在切换到后摄像头");
                 break;
             case CAMERA_DIRECTION_FRONT:
                 ToastUtils.showToast(context,"客官，请稍等，正在切换到前摄像头");
                 break;
         }
         this.currentDirection=direction;
         setCurrentCameraDirection(this.currentDirection);
         this.currentOperator.switchCameraDirectionOperate();
     }

    /**
     * 设置焦距比例，从设置焦距值
     * @param zoomProportion
     */
    public void setZoomProportion(float zoomProportion) {
        this.zoomProportion = zoomProportion;
        this.currentOperator.notifyFocusState();
    }
    public float getZoomProportion() {
        return zoomProportion;
    }
    public WorkThreadUtils getWorkThreadManager() {
        return workThreadManager;
    }
    public boolean isManualFocus() {
        return isManualFocus;
    }

    /**
     * 设置是否手动调焦
     * @param manualFocus
     */
    public void setManualFocus(boolean manualFocus) {
        this.isManualFocus = manualFocus;
        this.currentOperator.notifyFocusState();
    }

    public Context getContext() {
        return context;
    }
}
