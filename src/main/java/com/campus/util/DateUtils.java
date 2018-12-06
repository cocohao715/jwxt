package com.campus.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static boolean isNow(Date date) {
        //当前时间
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        //获取今天的日期
        String nowDay = sf.format(now);
        //对比的时间
        String day = sf.format(date);
        return day.equals(nowDay);
    }
    public static String getNowDateString(String format)
    {
        //当前时间
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat(format);
        //获取今天的日期
        String nowDay = sf.format(now);
        return  nowDay;
    }

    public static Date getNowDate()
    {
        //当前时间
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
        //获取今天的日期
        String nowDay = sf.format(now);
        try {
            now=sf.parse(nowDay);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return  now;
    }
    public static Date getDate(String date,String format) throws ParseException {

        SimpleDateFormat sf = new SimpleDateFormat(format);
        Date date1 =null;

            date1 = sf.parse(date);

        return  date1;
    }
}
