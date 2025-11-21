package com.xingen.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.xingen.camera.base.BaseActivity


/**
 * Created by ${xingen} on 2017/10/27.
 *
 *
 * 查看拍照图片
 */
class PictureActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, TAG + "onCreate")
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_picture
    }

    override fun initView(savedInstanceState: Bundle?) {
        val bundle = intent.extras
        if ((bundle != null) and bundle!!.containsKey(TAG)) {
            val url = bundle.getParcelable<Uri>(TAG)
            val imageView = findViewById<ImageView>(R.id.picture_show_iv)
            Glide.with(this).asBitmap().load(url).into(imageView)
        }
    }

    companion object {
        val TAG: String = PictureActivity::class.java.getSimpleName()

        fun openActivity(context: Context, uri: Uri ) {
            val bundle = Bundle()
            bundle.putParcelable(TAG, uri)
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }
    }
}
