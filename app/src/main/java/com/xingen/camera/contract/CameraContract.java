package com.xingen.camera.contract;

import android.view.TextureView;

/**
 * Created by ${xingen} on 2017/10/19.
 */

public interface CameraContract {

    interface Presenter {
        /**
         * 与View的onResume()生命周期保持一致
         */
        void onResume();

        /**
         * 与View的onPause()生命周期保持一致
         */
        void onPause();

        /**
         * 权限请求结果
         *
         * @param requestCode
         * @param permissions
         * @param grantResults
         */
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

        /**
         * 拍照或者录像
         */
        void takePictureOrVideo();

        /**
         * 切换模式： 拍照，录像
         * @param mode
         */
        void switchMode(int mode);

        /**
         * 切换摄像头，包含前后两种
         * @param direction
         */
        void switchCamera(int direction);

        /**
         * 暂停录制
         */
        void stopRecord();

        /**
         * 重新开始录制
         */
        void restartRecord();
    }

    interface View<T extends Presenter> {
        /**
         *  设置Presenter
         * @param t
         */
        void setPresenter(T t);
        /**
         * 获取TextureView
         * @return
         */
        TextureView getCameraView();
        /**
         * 加载拍照的图片路径
         *
         * @param filePath
         */
        void loadPictureResult(String filePath);

        /**
         * 显示计时时间
         * @param timing
         */
        void setTimingShow(String timing);

        /**
         *切换到录制状态
         * @param  mode
         *
         */
        void switchRecordMode(int mode);

        /**
         * 视频录制的三种状态
         */
        int MODE_RECORD_START=1;
        int MODE_RECORD_STOP=2;
        int MODE_RECORD_FINISH=3;

    }
}
