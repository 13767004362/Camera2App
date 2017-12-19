package com.xingen.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.xingen.camera.base.BaseActivity;
import com.xingen.camera.glide.GlideApp;

/**
 * Created by ${xingen} on 2017/10/27.
 * <p>
 * 查看拍照图片
 */

public class PictureActivity extends BaseActivity {
    public static final String TAG = PictureActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,TAG+"onCreate");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_picture;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null & bundle.containsKey(TAG)) {
            String url = bundle.getString(TAG);
            ImageView imageView = (ImageView) findViewById(R.id.picture_show_iv);
            GlideApp.with(this).asBitmap().load(url).into(imageView);
        }
    }
    public static void openActivity(Context context, String url) {
        Bundle bundle = new Bundle();
        bundle.putString(PictureActivity.TAG, url);
        Intent intent = new Intent(context, PictureActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }
}
