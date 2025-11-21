package com.xingen.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.xingen.camera.base.BaseVMActivity
import com.xingen.camera.camerax.CameraxUtils
import com.xingen.camera.camerax.VideoEncodeConfig
import com.xingen.camera.databinding.ActivityMainBinding
import com.xingen.camera.opengl.GLThread
import com.xingen.camera.view.systembar.SystemBarUtils.setStickyStyle
import java.io.File
import java.util.Locale

/**
 * Created by ${xinGen} on 2017/10/19.
 */
class MainActivity : BaseVMActivity<ActivityMainBinding, MainViewModel>() {
    var glThread: GLThread? = null

    // 默认后摄像头
    var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun initViewImpl(savedInstanceState: Bundle?) {
        binding.cameraRightTopController.setOnClickListener {
            if (viewModel.isRecording()) {
                // 停止录像
                glThread?.setEncode(false)
                viewModel.stopRecord()
            } else {
                // 开始录像
                viewModel.recordVideo()
                glThread?.setEncode(true)
            }
        }
        binding.cameraBtn.setOnClickListener {
            glThread?.takePhoto()
        }

        binding.cameraDirectionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.camera_direction_front) {
                // 切换到前摄像头
                if (cameraSelector != CameraSelector.DEFAULT_FRONT_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    startCamera()
                }
            } else {
                // 切换到后摄像头
                if (cameraSelector != CameraSelector.DEFAULT_BACK_CAMERA) {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    startCamera()
                }
            }
        }
        requestCameraPermission()
        initGLThread()
    }

    private fun requestCameraPermission() {
        if (allPermissionsGranted()) {
            // 如果已经有权限，则开启相机
            waitSurfaceViewReady()
        } else {
            // 如果没有权限，则请求权限
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private var timerSeconds = 0
    private val timerTask = object : Runnable {
        override fun run() {
            timerSeconds++
            // 格式化时间并更新 UI
            val minutes = timerSeconds / 60
            val seconds = timerSeconds % 60
            binding.cameraVideoRecordTipTimeTv.text =
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            // 每秒执行一次
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        //焦点改变的时候 设置隐藏系统状态栏
        setStickyStyle(window)
    }

    /**
     * 监听系统UI的显示，进行特殊处理
     */
    private fun setSystemUIChangeListener() {
        window.decorView.setOnSystemUiVisibilityChangeListener({ visibility ->
            //当系统UI显示的时候（例如输入法显示的时候），再次隐藏
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                setStickyStyle(window)
            }
        })
    }

    private var imageSurfaceTexture: SurfaceTexture? = null
    private fun initGLThread() {
        glThread = GLThread()
        val rotation = if (Build.VERSION.SDK_INT >= 30) {
            display.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
        Log.i(TAG, "activity rotation:$rotation")
        glThread?.setRotation(rotation)
        glThread?.onFrameAvailableListener = object : GLThread.OnFrameAvailableListener {
            override fun onFrameAvailable(imageSurfaceTexture: SurfaceTexture) {
                this@MainActivity.imageSurfaceTexture = imageSurfaceTexture
                // 等待纹理创建成后，设置到camerax 中preview中
                startCamera()
            }
        }
        glThread?.screenshotListener = object : GLThread.ScreenshotListener {
            override fun onScreenshotCaptured(bitmap: Bitmap) {
                viewModel.handleBitmapToGallery(bitmap)
            }
        }
    }

    override fun subViewModel() {
        viewModel.isRecordingState.observe(this) {
            if (it) {
                binding.cameraVideoRecordTipTimeTv.visibility = View.VISIBLE
                binding.cameraVideoRecordTipBg.visibility = View.VISIBLE
                binding.cameraRightTopController.setImageResource(R.drawable.camera_stop_iv)
                timerSeconds = 0
                mainHandler.postDelayed(timerTask, 1000)
            } else {
                binding.cameraVideoRecordTipTimeTv.let { i ->
                    i.text = ""
                    i.visibility = View.GONE
                }
                binding.cameraVideoRecordTipBg.visibility = View.GONE
                binding.cameraRightTopController.setImageResource(R.drawable.camera_init_iv)
                mainHandler.removeCallbacks(timerTask)
            }
        }
        viewModel.takePictureEvent.observe(this) {
            if (it.success) {
                val msg = "拍照成功"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(TAG, msg)
                Glide.with(this@MainActivity).load(it.uri!!).into(binding.cameraShow)
            } else {
                val msg = "拍照失败: ${it.error!!.message}"
                Log.e(TAG, msg, it.error)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }
        }
        viewModel.recordVideoEvent.observe(this){
            if (it){
                Toast.makeText(baseContext, "录制成功，保存相册成功", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(baseContext, "保存相册失败", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.isRecording()) {
            viewModel.stopRecord()
        }
        glThread?.stopRender()
    }
   var videoPath: String?=null
    private fun waitSurfaceViewReady() {
        videoPath = externalCacheDir!!.absolutePath + "/" + "test.mp4"
        viewModel.initEncode(
            videoPath!!,
            videoEncodeWidth,
            videoEncodeHeight,
            frameRate,
            bitRate,
            iFrameInterval
        )
        binding.cameraTextureView.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                    Log.d(TAG, "onSurfaceTextureAvailable size: ${width}x${height}")
                    glThread?.onSurfaceTextureSizeChanged(surface, width, height)
                    glThread?.setInputSurface(
                        viewModel.inputSurface(),
                        frameRate,
                        videoEncodeWidth,
                        videoEncodeHeight
                    )
                    glThread?.start()

                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    glThread?.onSurfaceTextureDestroyed(surface)
                    return false
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                }

            }
    }


    /**
     * 有相机权限后，开启相机
     */
    private fun startCamera() {
        //返回当前可以绑定生命周期的 ProcessCameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            //将相机的生命周期和activity的生命周期绑定，camerax 会自己释放，不用担心了
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // 选择camerax中最接近目标宽高尺寸
            val resolutionSelector =
                CameraxUtils.createResolutionSelector(
                    camera_width,
                    camera_height
                )
            //预览的 capture
            val preview = Preview.Builder()
                // 设置预览尺寸分辨率
                .setResolutionSelector(resolutionSelector)
                .build()

            try {
                // 在重新绑定之前，先解绑所有用例
                cameraProvider.unbindAll()
                // 将用例绑定到相机和当前生命周期
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

                preview.setSurfaceProvider {
                    // 这里使用opengl创建的外部纹理
                    val surfaceTexture: SurfaceTexture = imageSurfaceTexture!!
                    val size: Size = it.resolution
                    Log.d(TAG, "setSurfaceProvider preview size: $size")
                    surfaceTexture.setDefaultBufferSize(size.width, size.height)
                    val surface = Surface(surfaceTexture)
                    // 设置预览的 Surface
                    it.provideSurface(surface, ContextCompat.getMainExecutor(this)) {
                        // 在这里可以处理 surface 释放的回调
                        Log.d(TAG, "Surface released")
                        //surface.release()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "用例绑定失败", e)
                Toast.makeText(this, "启动相机失败", Toast.LENGTH_SHORT).show()
            }


        }, ContextCompat.getMainExecutor(this))

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                waitSurfaceViewReady()
            } else {
                Toast.makeText(
                    this.application,
                    "你没有授予相机权限。",
                    Toast.LENGTH_SHORT
                ).show()
                finish() // 如果没有权限，关闭应用
            }
        }
    }


    /**
     * 检查所有权限是否已授予
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG: String = MainActivity::class.java.getSimpleName()

        // 当前采用的1080p全高清,视频编码配置
        private val videoEncodeConfig = VideoEncodeConfig.FHD_1080P

        // 相机传感器是横屏,因此与手机预览和视频编码是相反的。
        private val camera_width = videoEncodeConfig.encodeHeight
        private val camera_height = videoEncodeConfig.encodeWidth

        //........ 视频编码配置信息

        // 录制视频的宽高是横屏
        val videoEncodeWidth = videoEncodeConfig.encodeHeight
        val videoEncodeHeight = videoEncodeConfig.encodeWidth
        val frameRate: Int = videoEncodeConfig.frameRate
        private val bitRate = videoEncodeConfig.bitRate
        private val iFrameInterval = videoEncodeConfig.iFrameInterval


        //........ 权限相关
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
            ).toTypedArray()


    }
}
