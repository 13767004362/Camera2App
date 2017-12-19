package com.xingen.camera.utils.time;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by ${xingen} on 2017/10/24.
 *
 *
 */

public class TimingUtils {


    /**
     * 时间格式化
     *
     * @param time
     * @return
     */
    public static String getDate(long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        Date data = new Date(time);
        return format.format(data);
    }
}
