package com.xingen.camera

import android.animation.Animator
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.xingen.camera.camera2.Camera2Manager
import com.xingen.camera.contract.CameraContract
import com.xingen.camera.mode.Constant
import com.xingen.camera.utils.animator.AnimatorBuilder
import com.xingen.camera.utils.toast.ToastUtils
import com.xingen.camera.view.widget.VerticalProgressBarLayout
import com.xingen.camera.widget.AutoFitTextureView

/**
 * Created by ${xinGen} on 2017/10/19.
 */
class CameraFragment : Fragment(), CameraContract.View<CameraContract.Presenter>, View.OnClickListener,
    RadioGroup.OnCheckedChangeListener, VerticalProgressBarLayout.VerticalMoveResultListener {
    private var show_result_iv: ImageView? = null
    private var controller_state_iv: ImageView? = null
    private var verticalProgressBarLayout: VerticalProgressBarLayout? = null
    private var textureView: AutoFitTextureView? = null
    private var show_record_tv: TextView? = null
    private var record_tip_circle: TextView? = null
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.rootView = inflater.inflate(R.layout.fragment_camera, container, false)
        initView()
        return this.rootView
    }

    override fun showToast(content: String?) {
        ToastUtils.showToastRunUIThread(getActivity(), content)
    }

    private fun initView() {
        this.textureView =
            rootView!!.findViewById(R.id.camera_auto_fit_texture_view)
        this.show_result_iv = rootView!!.findViewById<ImageView>(R.id.camera_show)
        this.show_record_tv =
            rootView!!.findViewById(R.id.camera_video_record_tip_time_tv)
        this.record_tip_circle = rootView!!.findViewById<TextView>(R.id.camera_video_record_tip_bg)
        this.rootView!!.findViewById<View>(R.id.camera_btn).setOnClickListener(this)
        this.verticalProgressBarLayout =
            rootView!!.findViewById(R.id.camera_vertical_progress_bar)
        this.controller_state_iv =
            this.rootView!!.findViewById(R.id.camera_right_top_controller)
        this.controller_state_iv!!.tag = CameraContract.View.MODE_RECORD_FINISH
        this.controller_state_iv!!.setOnClickListener(this)
        this.show_result_iv!!.setOnClickListener(this)
        (this.rootView!!.findViewById<View?>(R.id.camera_switch_radioGroup) as RadioGroup).setOnCheckedChangeListener(
            this
        )
        (this.rootView!!.findViewById<View?>(R.id.camera_direction_radioGroup) as RadioGroup).setOnCheckedChangeListener(
            this
        )
        this.verticalProgressBarLayout!!.verticalMoveResultListener = this
    }

    override fun onResume() {
        super.onResume()
        if (presenter != null) {
            presenter!!.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (presenter != null) {
            presenter!!.onPause()
        }
    }

    private var presenter: CameraContract.Presenter? = null

    override fun setPresenter(presenter: CameraContract.Presenter?) {
        this.presenter = presenter
    }

    override fun getCameraView(): TextureView? {
        return textureView
    }

    protected var filePath: String? = null

    override fun loadPictureResult(filePath: String?) {
        this.filePath = filePath
        Glide.with(this).asBitmap().load(filePath).into(show_result_iv!!)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.camera_btn -> this.presenter!!.takePictureOrVideo()
            R.id.camera_right_top_controller -> {
                val mode = controller_state_iv!!.tag as Int
                //录制状态中，可以暂停
                if (mode == CameraContract.View.MODE_RECORD_START) {
                    this.presenter!!.stopRecord()
                } //暂停状态，可以继续开始录制
                else if (mode == CameraContract.View.MODE_RECORD_STOP) {
                    this.presenter!!.restartRecord()
                }
            }

            R.id.camera_show -> if (!TextUtils.isEmpty(filePath)) {
                PictureActivity.openActivity(requireActivity(), filePath)
            }

            else -> {}
        }
    }

    override fun setTimingShow(timing: String?) {
        this.show_record_tv!!.text = timing
    }

    private var flashAnimator: Animator? = null

    override fun switchRecordMode(mode: Int) {
        when (mode) {
            CameraContract.View.MODE_RECORD_START -> {
                this.show_record_tv!!.visibility = View.VISIBLE
                this.record_tip_circle!!.visibility = View.VISIBLE
                this.controller_state_iv!!.setImageResource(R.drawable.camera_stop_iv)
                if (flashAnimator != null && flashAnimator!!.isRunning) {
                    flashAnimator!!.cancel()
                }
            }

            CameraContract.View.MODE_RECORD_STOP -> {
                this.controller_state_iv!!.setImageResource(R.drawable.camera_start_iv)
                this.show_record_tv!!.visibility = View.INVISIBLE
                flashAnimator = AnimatorBuilder.createFlashAnimator(this.record_tip_circle)
                flashAnimator!!.start()
            }

            CameraContract.View.MODE_RECORD_FINISH -> {
                this.show_record_tv!!.text = ""
                this.show_record_tv!!.visibility = View.GONE
                this.record_tip_circle!!.visibility = View.GONE
                this.controller_state_iv!!.setImageResource(R.drawable.camera_init_iv)
            }

            else -> {}
        }
        this.controller_state_iv!!.tag = mode
    }

    override fun onCheckedChanged(group: RadioGroup?, @IdRes checkedId: Int) {
        when (checkedId) {
            R.id.camera_video_record_btn -> this.presenter!!.switchMode(Constant.MODE_VIDEO_RECORD)
            R.id.camera_switch_picture_btn -> this.presenter!!.switchMode(Constant.MODE_CAMERA)
            R.id.camera_direction_front -> this.presenter!!.switchCamera(Camera2Manager.CAMERA_DIRECTION_FRONT)
            R.id.camera_direction_back -> this.presenter!!.switchCamera(Camera2Manager.CAMERA_DIRECTION_BACK)
            else -> {}
        }
    }

    override fun moveDistance(verticalBias: Float) {
        if (presenter != null) {
            presenter!!.setManualFocus(verticalBias)
        }
    }

    companion object {
        @JvmField
        val TAG: String = CameraFragment::class.java.getSimpleName()
        @JvmStatic
        fun newInstance(): CameraFragment {
            val cameraFragment = CameraFragment()
            return cameraFragment
        }
    }
}