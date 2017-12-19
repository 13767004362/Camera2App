package com.xingen.camera.camera2;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Log;
import android.view.TextureView;

import com.xingen.camera.camera2.operate.PictureOperater;
import com.xingen.camera.camera2.operate.VideoRecordOperator;
import com.xingen.camera.mode.Constant;
import com.xingen.camera.utils.thread.WorkThreadUtils;

/**
 * Created by ${xingen} on 2017/10/20.
 */

public class Camera2Manager {

    /**
     * 前后摄像头
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

    public Camera2Manager(Context context, WorkThreadUtils workThreadManager) {
        this.context = context;
        this.workThreadManager = workThreadManager;
        //默认使用后摄像头
        this.currentDirection=CAMERA_DIRECTION_BACK;
        this.pictureOperator = new PictureOperater(this.workThreadManager);
        this.videoRecordOperator = new VideoRecordOperator(this.workThreadManager);
        setCurrentCameraDirection(this.currentDirection);
        //默认拍照模式
        this.currentOperator = pictureOperator;
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
        Log.i(TAG,"setCurrentCameraDirection "+direction);
        this.videoRecordOperator.setCurrentDirection(direction);
        this.pictureOperator.setCurrentDirection(direction);
    }
    /**
     * 暂停视频
     */
    public  void pauseVideoRecord(){
        ((VideoRecordOperator) this.videoRecordOperator).pauseRecordingVideo();
    }
    public void switchMode(int currentMode) {
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
     public void switchCameraDirection(int direction){

         //相同摄像头方向，不进行操作
         if (currentDirection== direction){
               return;
         }
         //当视频录制状态，不能切换摄像头
         if (currentOperator instanceof  VideoRecordOperator){
             if (((VideoRecordOperator)currentOperator).isVideoRecord()){
                 return;
             }
         }
         Log.i(TAG," 切换模式 :"+direction);
         this.currentDirection=direction;
         setCurrentCameraDirection(this.currentDirection);
         this.currentOperator.switchCameraDirectionOperate();
     }

}
