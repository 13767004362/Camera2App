package com.xingen.camera.media.recod

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by HeXinGen  on 2025/11/4
 * Description:
 * 录音采集
 * 1个声道，pcm 16位,采样率 44100
 *
 */
class AudioCapture {
    interface AudioDataCallBack {
        fun onAudioData(data: ByteArray, size: Int)
    }

    private val isRecording = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null


    /**
     * 开始录音采集
     */
    @SuppressWarnings("all")
    fun start(audioDataCallBack: AudioDataCallBack) {
        if (isRecording.compareAndSet(false, true)) {
            // 获取系统推荐的最小缓冲区大小（以字节为单位），确保读写不会出现缓冲区不足
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                channelConfig, audioFormat, minBufferSize
            )
            audioThread.submit {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                //开始录制
                audioRecord?.startRecording()
                val buffer = ByteArray(minBufferSize)
                while (isRecording.get()) {
                    try {
                        val size = audioRecord!!.read(buffer, 0, buffer.size)
                        if (size > 0) {
                            // 通过callBack 传递pcm 数据
                            audioDataCallBack.onAudioData(buffer, size)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                audioRecord?.stop()
                audioRecord?.release()
            }
        }
    }

    /**
     * 停止
     */
    fun stop() {
        isRecording.set(false)
    }

    companion object {
        private val audioThread: ExecutorService = Executors.newSingleThreadExecutor()

        // 采样率：常用 44100Hz（CD 质量），也可根据需求调整（如 48000）
        const val sampleRate = 44100

        // 音频通道数
        const val audioChannel = 1

        // 输入声道配置：单声道 AudioFormat.CHANNEL_IN_MONO
        const val channelConfig = AudioFormat.CHANNEL_IN_MONO

        // 音频编码格式：PCM 16 位（常用且兼容性好）
        const val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    }
}