package com.xingen.camera.media.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import java.io.IOException

/**
 * Created by HeXinGen  on 2025/11/13
 * Description:
 *
 * 视频编码器采用的surface 模式,计算pts 会在opengl 中,
 * 通过EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, presentationTimeNs)。
 *
 */

class VideoEncoder(val videoWidth: Int, val videoHeight: Int ,val frameRate: Int,val bitRate: Int,val iFrameInterval: Int) : BaseSyncEncode(isAudio = false) {

    private var inputSurface: Surface? = null
    private val frameIntervalUs = 1000000L / frameRate // 每帧时间间隔,单位微妙
    fun getInputSurface(): Surface {
        return inputSurface!!
    }

    override fun initMediaCodec() {
        try {
            // 配置MediaCodec
            mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoWidth, videoHeight).apply {
                // 配置mediacodec 的 Surface模式
                this.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
                )
                // 配置码率
                 this.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                // 帧率
                this.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                // I帧/关键帧,间隔时间
                this.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            }


            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = mediaCodec!!.createInputSurface()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun handleEndOfInputStream() {
        // mediacodec的surface模式 ，发送流结束标识
        mediaCodec?.signalEndOfInputStream()
    }

    override fun checkPTS(bufferInfo: MediaCodec.BufferInfo) {
        // 确保时间戳单调递增
        if (bufferInfo.presentationTimeUs < lastPresentationTimeUs) {
            bufferInfo.presentationTimeUs = lastPresentationTimeUs + frameIntervalUs
        }
        lastPresentationTimeUs = bufferInfo.presentationTimeUs
    }


}