package com.xingen.camera;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.xingen.camera.base.BaseActivity;
import com.xingen.camera.contract.CameraContract;
import com.xingen.camera.presenter.CameraPresenter;
import com.xingen.camera.view.CameraFragment;

/**
 * Created by ${xinGen} on 2017/10/19.
 */
public class MainActivity extends BaseActivity {
    private CameraFragment cameraFragment;
    private CameraContract.Presenter presenter;
    public static final String TAG=MainActivity.class.getSimpleName();
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }
    @Override
    protected void initView(Bundle savedInstanceState) {
        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (cameraFragment == null) {
            cameraFragment = CameraFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.main_content_layout, cameraFragment, CameraFragment.TAG).commitAllowingStateLoss();
        }
        this.presenter = new CameraPresenter(cameraFragment);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.presenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG,TAG+" onPause");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG,TAG+" onSaveInstanceState ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,TAG+" onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,TAG+" onDestroy");
    }
}
