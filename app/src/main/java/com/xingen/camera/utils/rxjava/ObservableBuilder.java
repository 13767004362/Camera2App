package com.xingen.camera.utils.rxjava;

import android.content.Context;
import android.media.Image;

import com.xingen.camera.utils.file.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import rx.Observable;

/**
 * Created by ${xingen} on 2017/10/20.
 */

public class ObservableBuilder {
    /**
     * 合并多个视频文件，到一个新的视频中
     *
     * @param filePath1
     * @param filePath2
     * @return
     */
    public static Observable<String> createMergeMuiltFile(Context context,final String filePath1, final String filePath2) {
        return Observable.create(subscriber -> {
            String newFilePath = FileUtils.mergeMultipleVideoFile(context, filePath1, filePath2);
            subscriber.onNext(newFilePath);
        });
    }

    /**
     * 录制的视频文件，的存储路径
     *
     * @param videoPath
     * @return
     */
    public static Observable<String> createVideo(String videoPath) {
        return Observable.just(videoPath);
    }

    /**
     * 将JPEG图片的数据，写入磁盘中
     *
     * @param context
     * @param mImage
     * @return
     */
    public static Observable<String> createWriteCaptureImage(final Context context, final Image mImage) {
        Observable<String> observable = Observable.create(subscriber -> {
            File file = FileUtils.createPictureDiskFile(context, FileUtils.createBitmapFileName());
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(file);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            subscriber.onNext(file.getAbsolutePath());
        });
        return observable;
    }
}
