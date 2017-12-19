package com.xingen.camera.utils.file;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.xingen.camera.camera2.operate.VideoRecordOperator;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ${xinGen} on 2017/10/20.
 */

public class FileUtils {
    /**
     * 相片格式
     */
    public static final String PICTURE_FORMAT = ".png";
    /**
     * 视频格式
     */
    public static final String VIDEO_FORMAT = ".mp4";

    /**
     * 创建制定目录下的图片文件
     *
     * @param context
     * @param
     * @return
     */
    public static File createPictureDiskFile(Context context, String fileName) {
        return createDiskFile(context, Environment.DIRECTORY_PICTURES, fileName);
    }

    /**
     * 创建制定目录下的视频文件
     *
     * @param context
     * @param
     * @return
     */
    public static File createVideoDiskFile(Context context, String fileName) {
        return createDiskFile(context, Environment.DIRECTORY_MOVIES, fileName);
    }

    /**
     * getExternalFilesDir()提供的是私有的目录,不可见性，在app卸载后会被删除
     * <p>
     * getExternalCacheDir():提供外部缓存目录，是可见性的。
     *
     *  通过Context
     *
     *
     * @param context
     * @param dirName
     * @param fileName
     * @return
     */
    private static File createDiskFile(Context context, String dirName, String fileName) {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            cachePath = Environment.getExternalStoragePublicDirectory(dirName).getAbsolutePath();
        } else {
            cachePath = context.getFilesDir().getAbsolutePath();
        }
        return new File(cachePath + File.separator + fileName);
    }

    /**
     * 生成bitmap的文件名:日期，md5加密
     *
     * @return
     */
    public static String createBitmapFileName() {
        return createFileNameWithTime() + PICTURE_FORMAT;
    }

    /**
     * 创建Video的文件名
     *
     * @return
     */
    public static String createVideoFileName() {
        return createFileNameWithTime() + VIDEO_FORMAT;
    }

    /**
     * 以当前时间，加MD5编码后的文件名
     *
     * @return
     */
    private static String createFileNameWithTimeAndMD5() {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            String currentDate = createFileNameWithTime();
            mDigest.update(currentDate.getBytes("utf-8"));
            byte[] b = mDigest.digest();
            for (int i = 0; i < b.length; ++i) {
                String hex = Integer.toHexString(0xFF & b[i]);
                if (hex.length() == 1) {
                    stringBuilder.append('0');
                }
                stringBuilder.append(hex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String fileName = stringBuilder.toString();
        return fileName;

    }

    /**
     * 以当前时间，加MD5编码后的文件名
     *
     * @return
     */
    private static String createFileNameWithTime() {
        String currentDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return currentDate;
    }

    /**
     * 合并多个视频文件，成为一个，但会删除以前的旧文件。
     * <p>
     * 涉及到IO流操作，异步调用
     * <p>
     * 参考博客：
     * http://blog.csdn.net/thelastalien/article/details/51545323
     * <p>
     * http://blog.csdn.net/smile3670/article/details/41279749
     *
     * @param videoPath1
     * @param videoPath2
     * @return
     */
    public static String mergeMultipleVideoFile(Context context, String videoPath1, String videoPath2) {
        Log.i(VideoRecordOperator.TAG, " 需要合并的两个文件 " + videoPath1 + " 和 " + videoPath2);
        String currentFileName = createVideoDiskFile(context, createVideoFileName()).getAbsolutePath();
        try {
            //将视频文件转成对应的Movie
            List<String> fileList = new ArrayList<>();
            List<Movie> movieList = new ArrayList<>();
            fileList.add(videoPath1);
            fileList.add(videoPath2);
            for (String filePath : fileList) {
                movieList.add(MovieCreator.build(filePath));
            }
            //将多个Movie转成对应的音频和视频的mp4
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
            for (Movie movie : movieList) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals("soun")) {
                        audioTracks.add(track);
                    }
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
                }
            }
            //创建一个具备全部音频和视频的Movie
            Movie result = new Movie();
            if (audioTracks.size() > 0) {
                result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
            //写入新文件中，将数据
            writeMergeNewFile(currentFileName, result);
            //删除旧文件
            deleteFile(videoPath1);
            deleteFile(videoPath2);
            //释放资源
            fileList.clear();
            movieList.clear();
            Log.i(VideoRecordOperator.TAG, " 合并后的文件路径是 " + currentFileName + " 文件是否存在 " + new File(currentFileName).exists());
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(VideoRecordOperator.TAG, " 合并过程中发生异常： " + e.getMessage());
        }
        return currentFileName;
    }

    /**
     * 删除文件
     * 1. 单个文件
     * 2. 文件夹下的子文件夹和文件
     *
     * @param filePath
     */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            return;
        }
        //删除文件夹
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                if (childFile.isFile()) {
                    //删除文件
                    childFile.delete();
                } else if (file.isDirectory()) {
                    //递归删除子文件夹
                    deleteFile(childFile.getAbsolutePath());
                }
            }
            //删除文件夹本身
            file.delete();
        }//删除文件
        else {
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    /**
     * 写入合并数据到新文件中。
     *
     * @param filePath 新文件路径
     * @param result
     */
    private static void writeMergeNewFile(String filePath, Movie result) {
        FileOutputStream fileOutputStream = null;
        try {
            Container container = new DefaultMp4Builder().build(result);
            fileOutputStream = new FileOutputStream(filePath);
            container.writeContainer(fileOutputStream.getChannel());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
