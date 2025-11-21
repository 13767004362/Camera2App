package com.xingen.camera.media.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.xingen.camera.media.recod.AudioCapture


/**
 * Created by HeXinGen  on 2025/11/13
 * Description:
 * 音频编码器，将pcm 数据编码成aac
 */
class AudioEncoder : BaseSyncEncode(isAudio = true) {


    companion object {
        val TAG = "AudioEncoder"


        // 采样率44100HZ
        const val sampleRate: Int = AudioCapture.sampleRate

        //单声道
        const val channel: Int = AudioCapture.audioChannel
        //码率
        val BIT_RATE: Int = 128 * 1024
        val bytesPerSample = 2 // 16-bit = 2字节
    }


    override fun initMediaCodec() {
        // 配置MediaCodec
        mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            sampleRate,
            channel
        ).apply {
            this.setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            );
            this.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)

            this.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            this.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
    }

    override fun handleEndOfInputStream() {
      // mediacodec 的ByteBuffer模式，通过通过queueInputBuffer发送结束标记。
        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(1000)
        if (inputBufferIndex >= 0) {
            mediaCodec!!.queueInputBuffer(
                inputBufferIndex,
                0,
                0,  // size = 0 表示结束
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        }
    }


    /**
     * 添加pcm 音频 数据
     */
    fun addPcmData(data: ByteArray, size: Int) {
        if (!isRunning || mediaCodec == null) {
            return
        }
        if (size > 0) {
            // 将PCM数据送入编码器
            val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(1000)
            if (inputBufferIndex >= 0) {
                val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
                inputBuffer?.clear()
                inputBuffer?.put(data, 0, size)
                val durationUs = toPTS(size)
                mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, size, presentationTimeUs, 0)
                presentationTimeUs += durationUs
            }
        }
    }

    /**
     * 这里按照实际的采样数量(总量)与采样率 换算出pts 时间，单位微妙
     */
    fun toPTS(size: Int): Long {
        // 获取到pcm 数据的实际采样点数
        val samplesPerChannel = size / (channel * bytesPerSample)
        // 计算持续时间,单位微妙
        val durationUs = (1000000 * samplesPerChannel / sampleRate).toLong()
        return durationUs
    }

    override fun checkPTS(bufferInfo: MediaCodec.BufferInfo) {
        if (bufferInfo.presentationTimeUs < lastPresentationTimeUs) {
            bufferInfo.presentationTimeUs = lastPresentationTimeUs
        }
        lastPresentationTimeUs = bufferInfo.presentationTimeUs
    }


}