package com.xingen.camera

import android.graphics.Bitmap
import android.media.MediaCodec
import android.net.Uri
import android.view.Surface
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.xingen.camera.base.BaseApplication
import com.xingen.camera.base.BaseViewModel
import com.xingen.camera.media.encoder.AudioEncoder
import com.xingen.camera.media.encoder.BaseSyncEncode
import com.xingen.camera.media.encoder.VideoEncoder
import com.xingen.camera.media.muxer.MediaMuxerHandler
import com.xingen.camera.media.recod.AudioCapture
import com.xingen.camera.utils.GalleryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

/**
 * Created by HeXinGen  on 2025/11/20
 * Description:.
 */
class MainViewModel : BaseViewModel() {
    val takePictureEvent = MutableLiveData<TakePictureResult>()
    var isRecordingState = MutableLiveData<Boolean>()
    val  recordVideoEvent = MutableLiveData<Boolean>()
    private var videoEncoder: VideoEncoder? = null
    private var audioEncoder: AudioEncoder? = null
    private var mediaMuxerHandler: MediaMuxerHandler? = null
    private var audioCapture: AudioCapture? = null
    private var outputPath: String? = null


    fun initEncode(
        outputPath: String,
        videoEncodeWidth: Int,
        videoEncodeHeight: Int,
        frameRate: Int,
        bitRate: Int,
        iFrameInterval: Int
    ) {
        audioCapture = AudioCapture()
        mediaMuxerHandler = MediaMuxerHandler()
        this.outputPath = outputPath
        mediaMuxerHandler?.init(outputPath)
        audioEncoder = AudioEncoder()
        audioEncoder?.init(mediaMuxerHandler!!)
        videoEncoder =
            VideoEncoder(videoEncodeWidth, videoEncodeHeight, frameRate, bitRate, iFrameInterval)
        videoEncoder?.init(mediaMuxerHandler!!)
    }

    fun inputSurface(): Surface {
        return videoEncoder!!.getInputSurface()
    }

    /**
     * 开始录制
     */
    fun recordVideo() {
        isRecordingState.value = true
        audioCapture?.start(object : AudioCapture.AudioDataCallBack {
            override fun onAudioData(data: ByteArray, size: Int) {
                // 采集pcm数据 交给编码器处理
                audioEncoder?.addPcmData(data, size)
            }
        })
        audioEncoder?.start(object : BaseSyncEncode.EncodePacketCallBack {
            override fun onEncodePacket(
                byteBuffer: ByteBuffer,
                bufferInfo: MediaCodec.BufferInfo,
                isAudio: Boolean
            ) {
                // 编码器处理后的aac 交给混合器处理
                mediaMuxerHandler?.writeSampleData(byteBuffer, bufferInfo, isAudio)
            }

        })
        videoEncoder?.start(object : BaseSyncEncode.EncodePacketCallBack {
            override fun onEncodePacket(
                byteBuffer: ByteBuffer,
                bufferInfo: MediaCodec.BufferInfo,
                isAudio: Boolean
            ) {
                // 编码器处理后的h264 交给混合器处理
                mediaMuxerHandler?.writeSampleData(byteBuffer, bufferInfo, isAudio)
            }

        })
    }
    fun  isRecording():Boolean{
        return isRecordingState.value==true
    }

    fun  handleBitmapToGallery(bitmap: Bitmap){
        viewModelScope.launch(Dispatchers.IO){
            try {
                val uri = GalleryUtils.saveImageToGallery(BaseApplication.instance!!, bitmap)
                takePictureEvent.postValue(TakePictureResult(true, uri, null))
            } catch (e: Throwable) {
                takePictureEvent.postValue(TakePictureResult(false, null, e))
            }
        }
    }

    /**
     * 停止录制
     */
    fun stopRecord() {
        // 先停止音频流
        audioCapture?.stop()
        audioEncoder?.stop()
        videoEncoder?.stop()
        // 最后停止合成器
        mediaMuxerHandler?.stop()
        isRecordingState.value = false
        viewModelScope.launch(Dispatchers.IO) {
              try {
               GalleryUtils.saveMp4FileToGallery(BaseApplication.instance!!, outputPath!!)
                    recordVideoEvent.postValue(true)
              }catch (e: Exception){
                    recordVideoEvent.postValue(false)
              }finally {
                  // 删除临时文件
                  if (outputPath != null) {
                      File(outputPath!!).let {
                          if (it.exists()) it.delete()
                      }
                  }
              }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

}

data class TakePictureResult(
    val success: Boolean,
    val uri: Uri?,
    val error: Throwable?
)